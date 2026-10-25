# ---- Stage 1: restore + publish with the .NET SDK ----
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY PayrollService.csproj .
RUN dotnet restore
COPY . .
RUN dotnet publish -c Release -o /out --no-restore

# ---- Stage 2: runtime image (ASP.NET runtime only) ----
FROM mcr.microsoft.com/dotnet/aspnet:8.0
WORKDIR /app
COPY --from=build /out .
ENV ASPNETCORE_URLS=http://+:5100
# SQLite database lives in /data (a volume), owned by the non-root user the image provides
RUN mkdir -p /data && chown $APP_UID /data
USER $APP_UID
EXPOSE 5100
ENTRYPOINT ["dotnet", "PayrollService.dll"]
