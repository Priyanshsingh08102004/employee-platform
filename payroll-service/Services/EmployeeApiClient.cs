using System.Net.Http.Json;
using System.Text.Json;

namespace PayrollService.Services;

// Shape of the JSON the Java service returns for each employee. Extra fields in the JSON are ignored.
public record EmployeeDto(long Id, string Name, string Email, string? JobTitle, decimal Salary, string? DepartmentName);

// Typed HttpClient that talks to the Java employee-service. The base address and the X-API-KEY
// header are configured once in Program.cs.
public class EmployeeApiClient
{
    private const int MaxAttempts = 3;
    private static readonly JsonSerializerOptions Json = new(JsonSerializerDefaults.Web);

    private readonly HttpClient _http;

    public EmployeeApiClient(HttpClient http) => _http = http;

    public async Task<List<EmployeeDto>> GetEmployeesAsync(CancellationToken ct)
    {
        for (var attempt = 1; ; attempt++)
        {
            try
            {
                var result = await _http.GetFromJsonAsync<List<EmployeeDto>>("/api/internal/employees", Json, ct);
                return result ?? new List<EmployeeDto>();
            }
            // Retry only failures that might heal themselves (network blip, 5xx) - never 4xx like a wrong API key.
            catch (HttpRequestException ex) when (attempt < MaxAttempts && (ex.StatusCode is null || (int)ex.StatusCode.Value >= 500))
            {
                await Task.Delay(TimeSpan.FromMilliseconds(500 * attempt), ct);
            }
        }
    }
}
