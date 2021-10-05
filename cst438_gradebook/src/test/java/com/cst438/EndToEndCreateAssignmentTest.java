package com.cst438;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentGrade;
import com.cst438.domain.AssignmentGradeRepository;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Course;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;

@SpringBootTest
public class EndToEndCreateAssignmentTest {

	public static final String CHROME_DRIVER_FILE_LOCATION = "C:/chromedriver_win32/chromedriver.exe";

	public static final String URL = "https://sewilson-cst438grade-fe.herokuapp.com/";
	public static final String TEST_USER_EMAIL = "test@csumb.edu";
	public static final String TEST_INSTRUCTOR_EMAIL = "dwisneski@csumb.edu";
	public static final int SLEEP_DURATION = 1000; // 1 second.
	
	public static final String TEST_ASSIGNMENT_NAME = "Test Assignment";
	public static final String TEST_ASSIGNMENT_DATE = "2021-10-05";

	@Autowired
	EnrollmentRepository enrollmentRepository;

	@Autowired
	CourseRepository courseRepository;

	@Autowired
	AssignmentGradeRepository assignnmentGradeRepository;

	@Autowired
	AssignmentRepository assignmentRepository;

	@Test
	public void createAssignmentTest() throws Exception {

//		Database setup:  create course		
		Course c = new Course();
		c.setCourse_id(99999);
		c.setInstructor(TEST_INSTRUCTOR_EMAIL);
		c.setSemester("Fall");
		c.setYear(2021);
		c.setTitle("Test Course");

//	    add a student TEST into course 99999
		Enrollment e = new Enrollment();
		e.setCourse(c);
		e.setStudentEmail(TEST_USER_EMAIL);
		e.setStudentName("Test");

		courseRepository.save(c);
		e = enrollmentRepository.save(e);

		// set the driver location and start driver
		//@formatter:off
		// browser	property name 				Java Driver Class
		// edge 	webdriver.edge.driver 		EdgeDriver
		// FireFox 	webdriver.firefox.driver 	FirefoxDriver
		// IE 		webdriver.ie.driver 		InternetExplorerDriver
		//@formatter:on

		System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
		WebDriver driver = new ChromeDriver();
		// Puts an Implicit wait for 10 seconds before throwing exception
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

		driver.get(URL);
		Thread.sleep(SLEEP_DURATION);

		try {
			
			// Locate and click New Assignment button
			driver.findElement(By.xpath("//a[last()]")).click();
			Thread.sleep(SLEEP_DURATION);
			
			// Locate text fields and enter the data
			WebElement we = driver.findElement(By.xpath("/html/body/div/div/div/div[1]/div[1]/div/input"));
			we.sendKeys("99999");
			we = driver.findElement(By.xpath("/html/body/div/div/div/div[1]/div[2]/div/input")); 
			we.sendKeys(TEST_ASSIGNMENT_NAME);
			we = driver.findElement(By.xpath("/html/body/div/div/div/div[1]/div[3]/div/input"));
			we.sendKeys(TEST_ASSIGNMENT_DATE);
			
			// Locate submit button and click
			driver.findElement(By.xpath("/html/body/div/div/div/div[1]/button/span[1]")).click();
			Thread.sleep(SLEEP_DURATION);

			// verify that assignment has been added
			Assignment a = assignmentRepository.findByName(TEST_ASSIGNMENT_NAME).get(0);
			assertEquals(TEST_ASSIGNMENT_NAME, a.getName());
			assertEquals(99999, a.getCourse().getCourse_id());

		} catch (Exception ex) {
			throw ex;
		} finally {

			// clean up database.
			Assignment a = assignmentRepository.findByName(TEST_ASSIGNMENT_NAME).get(0);
			if (a!=null) assignmentRepository.delete(a);
			enrollmentRepository.delete(e);
			courseRepository.delete(c);

			driver.quit();
		}

	}
}
