namespace PayrollService.Models;

// One employee's payslip inside a run. Employee details are COPIED in (a snapshot), not linked,
// because this service has its own database - it must not depend on the Java database existing.
public class PayrollRecord
{
    public int Id { get; set; }
    public int PayrollRunId { get; set; }

    public long EmployeeId { get; set; }
    public string EmployeeName { get; set; } = "";
    public string EmployeeEmail { get; set; } = "";
    public string Department { get; set; } = "";

    public decimal AnnualSalary { get; set; }
    public decimal MonthlyGross { get; set; }
    public decimal Basic { get; set; }
    public decimal Hra { get; set; }
    public decimal SpecialAllowance { get; set; }
    public decimal ProvidentFund { get; set; }
    public decimal IncomeTax { get; set; }
    public decimal NetPay { get; set; }
}
