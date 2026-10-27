using Microsoft.EntityFrameworkCore;
using PayrollService.Models;

namespace PayrollService.Data;

// Entity Framework Core is the .NET counterpart of JPA/Hibernate: classes map to tables,
// and LINQ queries are translated to SQL.
public class PayrollDbContext : DbContext
{
    public PayrollDbContext(DbContextOptions<PayrollDbContext> options) : base(options) { }

    public DbSet<PayrollRun> PayrollRuns => Set<PayrollRun>();
    public DbSet<PayrollRecord> PayrollRecords => Set<PayrollRecord>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<PayrollRun>(run =>
        {
            run.HasIndex(r => r.Month).IsUnique();   // database-level guarantee: no double payroll for a month
            run.HasMany(r => r.Records).WithOne().HasForeignKey(x => x.PayrollRunId);
        });
    }
}
