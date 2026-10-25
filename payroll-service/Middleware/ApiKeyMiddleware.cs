using System.Security.Cryptography;
using System.Text;

namespace PayrollService.Middleware;

// Every /api/* request must carry the shared secret in the X-API-KEY header. Health checks and
// Swagger pages are outside /api, so they stay open for Docker/Kubernetes probes.
public class ApiKeyMiddleware
{
    private readonly RequestDelegate _next;
    private readonly byte[] _expected;

    public ApiKeyMiddleware(RequestDelegate next, IConfiguration config)
    {
        _next = next;
        var key = config["Internal:ApiKey"]
                  ?? throw new InvalidOperationException("Internal:ApiKey is not configured");
        _expected = Encoding.UTF8.GetBytes(key);
    }

    public async Task InvokeAsync(HttpContext context)
    {
        if (!context.Request.Path.StartsWithSegments("/api"))
        {
            await _next(context);
            return;
        }

        if (context.Request.Headers.TryGetValue("X-API-KEY", out var provided) &&
            CryptographicOperations.FixedTimeEquals(Encoding.UTF8.GetBytes(provided.ToString()), _expected))
        {
            await _next(context);
            return;
        }

        context.Response.StatusCode = StatusCodes.Status401Unauthorized;
        await context.Response.WriteAsJsonAsync(new { message = "Missing or invalid API key" });
    }
}
