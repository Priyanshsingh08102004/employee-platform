namespace PayrollService.Services;

public record PayslipBreakdown(
    decimal MonthlyGross,
    decimal Basic,
    decimal Hra,
    decimal SpecialAllowance,
    decimal ProvidentFund,
    decimal IncomeTax,
    decimal NetPay);

/// <summary>
/// Pure calculation logic with no database or HTTP in it, so it is trivial to unit test.
/// ILLUSTRATIVE: the slabs below are simplified for a portfolio project and are NOT tax advice.
/// </summary>
public static class PayrollCalculator
{
    private const decimal BasicShare = 0.40m;        // basic pay = 40% of gross
    private const decimal HraShare = 0.20m;          // house rent allowance = 20% of gross
    private const decimal PfRate = 0.12m;            // employee provident fund = 12% of basic
    private const decimal StandardDeduction = 75_000m;
    private const decimal RebateLimit = 700_000m;    // taxable income up to this pays no tax

    // (upper bound of slab, rate). Income inside each slab is taxed at that slab's rate.
    private static readonly (decimal Upto, decimal Rate)[] Slabs =
    {
        (300_000m, 0.00m),
        (700_000m, 0.05m),
        (1_000_000m, 0.10m),
        (1_200_000m, 0.15m),
        (1_500_000m, 0.20m),
        (decimal.MaxValue, 0.30m),
    };

    public static PayslipBreakdown Calculate(decimal annualSalary)
    {
        if (annualSalary <= 0)
            throw new ArgumentOutOfRangeException(nameof(annualSalary), "Annual salary must be greater than zero.");

        var gross = Round(annualSalary / 12m);
        var basic = Round(gross * BasicShare);
        var hra = Round(gross * HraShare);
        var special = gross - basic - hra;                 // whatever is left of gross
        var pf = Round(basic * PfRate);

        var taxableAnnual = Math.Max(0m, annualSalary - StandardDeduction);
        var monthlyTax = Round(AnnualTax(taxableAnnual) / 12m);

        var net = gross - pf - monthlyTax;
        return new PayslipBreakdown(gross, basic, hra, special, pf, monthlyTax, net);
    }

    private static decimal AnnualTax(decimal taxable)
    {
        if (taxable <= RebateLimit) return 0m;

        decimal tax = 0m, lower = 0m;
        foreach (var (upto, rate) in Slabs)
        {
            if (taxable <= lower) break;
            var portionInSlab = Math.Min(taxable, upto) - lower;
            tax += portionInSlab * rate;
            lower = upto;
        }
        return tax;
    }

    private static decimal Round(decimal value) => Math.Round(value, 2, MidpointRounding.AwayFromZero);
}
