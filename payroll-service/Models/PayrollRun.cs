namespace PayrollService.Models;

// One payroll run = "we paid everyone for month X". Only one run per month is allowed.
public class PayrollRun
{
    public int Id { get; set; }
    public string Month { get; set; } = "";          // e.g. "2026-10"
    public DateTime CreatedAtUtc { get; set; }
    public int EmployeeCount { get; set; }
    public decimal TotalNetPay { get; set; }
    public List<PayrollRecord> Records { get; set; } = new();
}
