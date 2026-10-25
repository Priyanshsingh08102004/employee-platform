package com.example.employees.service;

import org.springframework.stereotype.Service;

// NOTE: this is a rule-based assistant (keyword matching -> canned answer), not a real
// LLM. It's built this way on purpose - it works fully offline with no API key, and the
// request/response shape (ChatbotController.ask -> this.reply -> JSON back to the browser)
// is exactly what you'd keep if you later swapped this method's body for a real API call
// to an LLM provider.
@Service
public class ChatbotService {

    public String reply(String message) {
        if (message == null || message.isBlank()) {
            return "Ask me something like \"how do I apply\" or \"how do I add an employee\".";
        }
        String m = message.toLowerCase();

        if (m.contains("payroll") || m.contains("payslip") || m.contains("salary slip")) {
            return "Admins can open Payroll from the dashboard, pick a month, and run it. A separate .NET service calculates each employee's monthly gross, provident fund, income tax and net pay.";
        }
        if (m.contains("apply") || m.contains("application") || m.contains("submit")) {
            return "To apply, go to the Apply page, fill in your details, attach your resume, and submit. A manager will review it.";
        }
        if (m.contains("status") || m.contains("pending") || m.contains("approved") || m.contains("rejected")) {
            return "Pending means a manager hasn't reviewed your application yet. Approved means your employee record was created and you'll get an email. Rejected means it wasn't accepted this time, usually with a reason.";
        }
        if (m.contains("add employee") || m.contains("create employee")) {
            return "Click \"Add employee\" on the dashboard, fill in the form, and save. Both admins and staff accounts can add employees directly.";
        }
        if (m.contains("delete")) {
            return "Only Admin accounts can delete employees. Deletes are soft deletes - the record is hidden, not erased.";
        }
        if (m.contains("search") || m.contains("filter")) {
            return "Use the search box to look up by name, email, department or title, and the department dropdown to filter the list.";
        }
        if (m.contains("export") || m.contains("csv")) {
            return "Click \"Export CSV\" in the dashboard header to download every active employee as a spreadsheet file.";
        }
        if (m.contains("photo") || m.contains("picture") || m.contains("image")) {
            return "When adding or editing an employee, use the \"Profile photo\" field to upload an image.";
        }
        if (m.contains("review") || m.contains("manager")) {
            return "Admin accounts see a \"Review Applications\" link where they can approve or reject submitted applications, and correct details before approving.";
        }
        if (m.contains("email")) {
            return "Applicants get an email automatically when a manager approves or rejects their application.";
        }
        if (m.contains("hello") || m.contains("hi")) {
            return "Hi! I can help with applying, checking status, searching, or managing employees. What do you need?";
        }
        return "I'm not sure about that yet. Try asking about applying, application status, searching, or adding/deleting employees.";
    }
}
