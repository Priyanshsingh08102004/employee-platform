using PayrollService.Services;
using Xunit;

namespace PayrollService.Tests;

public class PayrollCalculatorTests
{
    [Fact]
    public void Salary_AboveRebate_IsTaxedBySlab()
    {
        // 12,00,000 a year -> taxable 11,25,000 -> slab tax 68,750 a year -> 5,729.17 a month
        var p = PayrollCalculator.Calculate(1_200_000m);

        Assert.Equal(100_000.00m, p.MonthlyGross);
        Assert.Equal(40_000.00m, p.Basic);
        Assert.Equal(20_000.00m, p.Hra);
        Assert.Equal(40_000.00m, p.SpecialAllowance);
        Assert.Equal(4_800.00m, p.ProvidentFund);
        Assert.Equal(5_729.17m, p.IncomeTax);
        Assert.Equal(89_470.83m, p.NetPay);
    }

    [Fact]
    public void Salary_BelowRebateLimit_PaysNoTax()
    {
        var p = PayrollCalculator.Calculate(600_000m);

        Assert.Equal(50_000.00m, p.MonthlyGross);
        Assert.Equal(0m, p.IncomeTax);
        Assert.Equal(2_400.00m, p.ProvidentFund);
        Assert.Equal(47_600.00m, p.NetPay);
    }

    [Fact]
    public void HighSalary_ReachesTopSlab()
    {
        // 24,00,000 a year -> taxable 23,25,000 -> 3,87,500 a year -> 32,291.67 a month
        var p = PayrollCalculator.Calculate(2_400_000m);

        Assert.Equal(200_000.00m, p.MonthlyGross);
        Assert.Equal(32_291.67m, p.IncomeTax);
        Assert.Equal(158_108.33m, p.NetPay);
    }

    [Fact]
    public void Components_AlwaysAddBackUpToGross()
    {
        var p = PayrollCalculator.Calculate(987_654m);

        Assert.Equal(p.MonthlyGross, p.Basic + p.Hra + p.SpecialAllowance);
    }

    [Theory]
    [InlineData(0)]
    [InlineData(-1)]
    public void NonPositiveSalary_IsRejected(int salary)
    {
        Assert.Throws<ArgumentOutOfRangeException>(() => PayrollCalculator.Calculate(salary));
    }
}
