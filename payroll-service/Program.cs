using System.Text.RegularExpressions;
using Microsoft.EntityFrameworkCore;
using Microsoft.OpenApi.Models;
using PayrollService.Data;
using PayrollService.Middleware;
using PayrollService.Models;
using PayrollService.Services;

var builder = WebApplication.CreateBuilder(args);

// Local dev default. Containers set ASPNETCORE_URLS themselves, which we must not override.
if (string.IsNullOrEmpty(Environment.GetEnvironmentVariable("ASPNETCORE_URLS")))
    builder.WebHost.UseUrls("http://localhost:5100");

builder.Services.AddDbContext<PayrollDbContext>(o =>
    o.UseSqlite(builder.Configuration.GetConnectionString("Payroll") ?? "Data Source=payroll.db"));

// Typed HttpClient: base URL + shared secret + timeout, configured once.
builder.Services.AddHttpClient<EmployeeApiClient>(client =>
{
    client.BaseAddress = new Uri(builder.Configuration["EmployeeService:BaseUrl"]
                                 ?? throw new InvalidOperationException("EmployeeService:BaseUrl is not configured"));
    client.DefaultRequestHeaders.Add("X-API-KEY", builder.Configuration["Internal:ApiKey"]);
    client.Timeout = TimeSpan.FromSeconds(10);
});

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(o =>
{
    o.SwaggerDoc("v1", new OpenApiInfo { Title = "Payroll Service", Version = "v1" });
    o.AddSecurityDefinition("ApiKey", new OpenApiSecurityScheme
    {
        Name = "X-API-KEY",
        In = ParameterLocation.Header,
        Type = SecuritySchemeType.ApiKey,
        Description = "Shared secret between the Java and .NET services"
    });
    o.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme { Reference = new OpenApiReference { Type = ReferenceType.SecurityScheme, Id = "ApiKey" } },
            Array.Empty<string>()
        }
    });
});

var app = builder.Build();

// EnsureCreated builds the tables on first start. A larger project would use EF Core migrations instead.
using (var scope = app.Services.CreateScope())
{
    scope.ServiceProvider.GetRequiredService<PayrollDbContext>().Database.EnsureCreated();
}

app.UseSwagger();
app.UseSwaggerUI();
app.UseMiddleware<ApiKeyMiddleware>();

var monthPattern = new Regex(@"^\d{4}-(0[1-9]|1[0-2])$");

app.MapGet("/health", () => Results.Ok(new { status = "UP", service = "payroll-service" }));

// Run payroll for a month: pull employees from the Java service, calculate, store the result.
app.MapPost("/api/payroll/runs", async (
    string month,
    PayrollDbContext db,
    EmployeeApiClient employees,
    ILogger<Program> log,
    CancellationToken ct) =>
{
    if (!monthPattern.IsMatch(month))
        return Results.BadRequest(new { message = "month must look like 2026-10" });

    if (await db.PayrollRuns.AnyAsync(r => r.Month == month, ct))
        return Results.Conflict(new { message = $"Payroll for {month} has already been run" });

    List<EmployeeDto> staff;
    try
    {
        staff = await employees.GetEmployeesAsync(ct);
    }
    catch (HttpRequestException ex)
    {
        log.LogError(ex, "Could not fetch employees from the employee service");
        return Results.Json(new { message = "Could not reach the employee service" }, statusCode: StatusCodes.Status502BadGateway);
    }

    if (staff.Count == 0)
        return Results.BadRequest(new { message = "There are no employees to pay" });

    var run = new PayrollRun { Month = month, CreatedAtUtc = DateTime.UtcNow };
    foreach (var e in staff)
    {
        var p = PayrollCalculator.Calculate(e.Salary);
        run.Records.Add(new PayrollRecord
        {
            EmployeeId = e.Id,
            EmployeeName = e.Name,
            EmployeeEmail = e.Email,
            Department = e.DepartmentName ?? "",
            AnnualSalary = e.Salary,
            MonthlyGross = p.MonthlyGross,
            Basic = p.Basic,
            Hra = p.Hra,
            SpecialAllowance = p.SpecialAllowance,
            ProvidentFund = p.ProvidentFund,
            IncomeTax = p.IncomeTax,
            NetPay = p.NetPay
        });
    }
    run.EmployeeCount = run.Records.Count;
    run.TotalNetPay = run.Records.Sum(r => r.NetPay);   // summed in memory: SQLite cannot aggregate decimals in SQL

    db.PayrollRuns.Add(run);
    await db.SaveChangesAsync(ct);
    log.LogInformation("Payroll {Month} completed for {Count} employees", month, run.EmployeeCount);
    return Results.Created($"/api/payroll/runs/{run.Id}", run);
});

app.MapGet("/api/payroll/runs", async (PayrollDbContext db, CancellationToken ct) =>
    Results.Ok(await db.PayrollRuns
        .OrderByDescending(r => r.Id)
        .Select(r => new { r.Id, r.Month, r.CreatedAtUtc, r.EmployeeCount, r.TotalNetPay })
        .ToListAsync(ct)));

app.MapGet("/api/payroll/runs/{id:int}", async (int id, PayrollDbContext db, CancellationToken ct) =>
{
    var run = await db.PayrollRuns.Include(r => r.Records).FirstOrDefaultAsync(r => r.Id == id, ct);
    return run is null ? Results.NotFound(new { message = $"Payroll run {id} not found" }) : Results.Ok(run);
});

app.Run();

public partial class Program { }
