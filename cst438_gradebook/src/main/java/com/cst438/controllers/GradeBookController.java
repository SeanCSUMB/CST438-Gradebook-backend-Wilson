package com.cst438.controllers;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentListDTO;
import com.cst438.domain.AssignmentGrade;
import com.cst438.domain.AssignmentGradeRepository;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Course;
import com.cst438.domain.CourseDTOG;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.GradebookDTO;
//import com.cst438.services.RegistrationService;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "https://sewilson-cst438grade-fe.herokuapp.com/"})
public class GradeBookController {
	
	@Autowired
	AssignmentRepository assignmentRepository;
	
	@Autowired
	AssignmentGradeRepository assignmentGradeRepository;
	
	@Autowired
	EnrollmentRepository enrollmentRepository;
	
	@Autowired
	CourseRepository courseRepository;
	
	//@Autowired
	//RegistrationService registrationService;
	
	//check the type of account of the user; 1 is instructor and 0 is student
	@GetMapping("/verify")
	public int checkUserType(@AuthenticationPrincipal OAuth2User principal) {
		String email = principal.getAttribute("email");  // user name
		List<Enrollment> enrollments = enrollmentRepository.findByStudentEmail(email);
		
		if(enrollments.size() == 0) {
			return 1;
		}else {
			return 0;
		}
		
	}
	
	// get assignments for an instructor that need grading, or assignments and their grades for a student
	@GetMapping("/gradebook")
	public AssignmentListDTO getAssignmentsNeedGrading(@AuthenticationPrincipal OAuth2User principal) {
		
		String email = principal.getAttribute("email");  // user name
		
		AssignmentListDTO result = new AssignmentListDTO();
		
		//check if the user is enrolled in classes; if not, user is an instructor.
		List<Enrollment> enrollments = enrollmentRepository.findByStudentEmail(email);
		if(enrollments.size() == 0) {
			//Find instrutor's ungraded assignments
			List<Assignment> assignments = assignmentRepository.findNeedGradingByEmail(email);
			int avgGrade;
			for (Assignment a: assignments) {
				//avgGrade = getAverageGrade(a.getId());
				result.assignments.add(new AssignmentListDTO.AssignmentDTO(a.getId(), a.getCourse().getCourse_id(), a.getName(), a.getDueDate().toString() , a.getCourse().getTitle(), "___"));
			}
		}else {
			//Find all of the student's assignments
			Course course;
			List<Assignment> assignments;
			for (Enrollment e: enrollments) {
				course = e.getCourse();
				assignments = course.getAssignments();
				String grade;
				for (Assignment a: assignments) {
					grade = assignmentGradeRepository.findByAssignmentIdAndStudentEmail(a.getId(), email).getScore();
					result.assignments.add(new AssignmentListDTO.AssignmentDTO(a.getId(), a.getCourse().getCourse_id(), a.getName(), a.getDueDate().toString() , a.getCourse().getTitle(), grade));
				}
			}	
		}
		
		return result;
	}
	
	@GetMapping("/gradebook/{id}")
	public GradebookDTO getGradebook(@PathVariable("id") Integer assignmentId, @AuthenticationPrincipal OAuth2User principal) {
		
		String email = principal.getAttribute("email");  // user name (should be instructor's email) 
		Assignment assignment = checkAssignment(assignmentId, email);
		
		// get the enrollment for the course
		//  for each student, get the current grade for assignment, 
		//   if the student does not have a current grade, create an empty grade
		GradebookDTO gradebook = new GradebookDTO();
		gradebook.assignmentId= assignmentId;
		gradebook.assignmentName = assignment.getName();
		for (Enrollment e : assignment.getCourse().getEnrollments()) {
			GradebookDTO.Grade grade = new GradebookDTO.Grade();
			grade.name = e.getStudentName();
			grade.email = e.getStudentEmail();
			// does student have a grade for this assignment
			AssignmentGrade ag = assignmentGradeRepository.findByAssignmentIdAndStudentEmail(assignmentId,  grade.email);
			if (ag != null) {
				grade.grade = ag.getScore();
				grade.assignmentGradeId = ag.getId();
			} else {
				grade.grade = "";
				AssignmentGrade agNew = new AssignmentGrade(assignment, e);
				agNew = assignmentGradeRepository.save(agNew);
				grade.assignmentGradeId = agNew.getId();  // key value generated by database on save.
			}
			gradebook.grades.add(grade);
		}
		return gradebook;
	}
	
	// interconnectivity is not needed for this assignment, so this function is disabled
	/*@PostMapping("/course/{course_id}/finalgrades")
	@Transactional
	public void calcFinalGrades(@PathVariable int course_id, @AuthenticationPrincipal OAuth2User principal) {
		System.out.println("Gradebook - calcFinalGrades for course " + course_id);
		
		// check that this request is from the course instructor 
		String email = principal.getAttribute("email");  // user name (should be instructor's email) 
		
		Course c = courseRepository.findByCourse_id(course_id);
		if (!c.getInstructor().equals(email)) {
			throw new ResponseStatusException( HttpStatus.UNAUTHORIZED, "Not Authorized. " );
		}
		
		CourseDTOG cdto = new CourseDTOG();
		cdto.course_id = course_id;
		cdto.grades = new ArrayList<>();
		for (Enrollment e: c.getEnrollments()) {
			double total=0.0;
			int count = 0;
			for (AssignmentGrade ag : e.getAssignmentGrades()) {
				count++;
				total = total + Double.parseDouble(ag.getScore());
			}
			double average = total/count;
			CourseDTOG.GradeDTO gdto = new CourseDTOG.GradeDTO();
			gdto.grade=letterGrade(average);
			gdto.student_email=e.getStudentEmail();
			gdto.student_name=e.getStudentName();
			cdto.grades.add(gdto);
			System.out.println("Course="+course_id+" Student="+e.getStudentEmail()+" grade="+gdto.grade);
		}
		registrationService.sendFinalGrades(course_id, cdto);
	}
	private String letterGrade(double grade) {
		if (grade >= 90) return "A";
		if (grade >= 80) return "B";
		if (grade >= 70) return "C";
		if (grade >= 60) return "D";
		return "F";
	}*/
	
	@PutMapping("/gradebook/{id}")
	@Transactional
	public void updateGradebook (@RequestBody GradebookDTO gradebook, @PathVariable("id") Integer assignmentId, @AuthenticationPrincipal OAuth2User principal) {
		
		String email = principal.getAttribute("email");  // user name (should be instructor's email) 
		checkAssignment(assignmentId, email);  // check that user name matches instructor email of the course.
		
		// for each grade in gradebook, update the assignment grade in database 
		
		for (GradebookDTO.Grade g : gradebook.grades) {
			AssignmentGrade ag = assignmentGradeRepository.findById(g.assignmentGradeId);
			if (ag == null) {
				throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Invalid grade primary key. "+g.assignmentGradeId);
			}
			ag.setScore(g.grade);
			assignmentGradeRepository.save(ag);
		}
		
	}
	
	//change an assignment's name
	@PutMapping("/gradebook/{id}/{newName}")
	@Transactional
	public void updateAssignmentName (@PathVariable("id") Integer assignmentId, @PathVariable("newName") String newName, @AuthenticationPrincipal OAuth2User principal ) {
		
		String email = principal.getAttribute("email");  // user name (should be instructor's email) 
		Assignment assignment = checkAssignment(assignmentId, email);  // check that user name matches instructor email of the course.
		
		assignment.setName(newName);
		assignmentRepository.save(assignment);
		
		
	}
	
	// delete an assignment
	@DeleteMapping("/gradebook/{id}")
	@Transactional
	public void deleteAssignment(  @PathVariable("id") Integer assignmentId, @AuthenticationPrincipal OAuth2User principal  ) {
		
		String email = principal.getAttribute("email");   // instructor's email 
		
		Assignment assignment = checkAssignment(assignmentId, email); //ensure that the instructor is the instructor of the course
		
		//reject deletion if assignment is graded
		if (assignment.getNeedsGrading() == 1) {
			throw  new ResponseStatusException( HttpStatus.BAD_REQUEST, "Assignments with existing grades cannot be deleted.");
		}
		
		// get the enrollment for the course
		//  for each student, if an (empty) grade file exists, such as those created by get gradebook/{id}, delete it
		GradebookDTO gradebook = new GradebookDTO();
		gradebook.assignmentId= assignmentId;
		gradebook.assignmentName = assignment.getName();
		for (Enrollment e : assignment.getCourse().getEnrollments()) {
			GradebookDTO.Grade grade = new GradebookDTO.Grade();
			grade.name = e.getStudentName();
			grade.email = e.getStudentEmail();
			// does student have a grade for this assignment
			AssignmentGrade ag = assignmentGradeRepository.findByAssignmentIdAndStudentEmail(assignmentId,  grade.email);
			if (ag != null) {
				assignmentGradeRepository.delete(ag);
			}
		}
		// finally delete the assignment
		assignmentRepository.delete(assignment);
	}
	
	// create an assignment
	@PostMapping("/gradebook")
	@Transactional
	public Assignment addAssignment(@RequestParam("courseId") Integer courseId, @RequestParam("name") String name, @RequestParam("due") String due, @AuthenticationPrincipal OAuth2User principal) { 
		
			String email = principal.getAttribute("email");   // instructor's email 
		
			Course course  = courseRepository.findByCourse_id(courseId);
			
			//check that the user is the instructor of the course
			if (!course.getInstructor().equals(email)) {
				throw new ResponseStatusException( HttpStatus.UNAUTHORIZED, "Not Authorized. " );
			}
			
			//create and save the assignment
			Assignment assignment = new Assignment();
			assignment.setCourse(course);
			assignment.setDueDate(Date.valueOf(due));
			assignment.setName(name);
			assignmentRepository.save(assignment);

			return assignment;
	}
	
	private Assignment checkAssignment(int assignmentId, String email) {
		// get assignment 
		Assignment assignment = assignmentRepository.findById(assignmentId);
		if (assignment == null) {
			throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "Assignment not found. "+assignmentId );
		}
		// check that user is the course instructor
		if (!assignment.getCourse().getInstructor().equals(email)) {
			throw new ResponseStatusException( HttpStatus.UNAUTHORIZED, "Not Authorized. " );
		}
		
		return assignment;
	}
	
}
