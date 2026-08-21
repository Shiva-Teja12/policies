import com.practice.springbootdemo.advance_performance_module.DTOs.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.DTOs.common.PagedResponse;
import com.practice.springbootdemo.advance_performance_module.DTOs.hr.CreateManagerRequest;
import com.practice.springbootdemo.advance_performance_module.DTOs.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.DTOs.search.EmployeeSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.service.hr.EmployeeManagementService;
import com.practice.springbootdemo.advance_performance_module.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/employees")
@PreAuthorize("hasRole('HR')")
@Tag(name = "Employee Management", description = "HR Employee & Manager Management APIs")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeManagementController {
    private final EmployeeManagementService service;
    private final SearchService searchService;
    public EmployeeManagementController(EmployeeManagementService service, SearchService searchService) {
        this.service = service;
        this.searchService = searchService;
    }
    @GetMapping
    @Operation(summary = "Get All Employees", description = "Retrieve all active employees (HR only)")
    public ApiResponse<List<EmployeeResponse>> employees() {
        return ApiResponse.success(service.getEmployees());
    }
    @GetMapping("/managers")
    @Operation(summary = "Get All Managers", description = "Retrieve all active managers (HR only)")
    public ApiResponse<List<EmployeeResponse>> managers() {
        return ApiResponse.success(service.getManagers());
    }
    @PostMapping("/managers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create Manager", description = "Register and provision a new Manager account (HR only)")
    public ApiResponse<EmployeeResponse> createManager(@Valid @RequestBody CreateManagerRequest request) {
        return ApiResponse.success("Manager account created successfully", service.createManager(request));
    }
    @GetMapping("/search")
    @Operation(
            summary = "Search Employees (HR)",
            description = "Dynamic multi-filter search with sorting and 0-based pagination for HR"
    )
    public ApiResponse<PagedResponse<EmployeeResponse>> searchEmployees(EmployeeSearchCriteria criteria) {
        return ApiResponse.success(searchService.searchEmployees(criteria));
    }
}