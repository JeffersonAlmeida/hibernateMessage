//$Id: Student.java,v 1.3 2005/02/12 07:27:21 steveebersole Exp $
package org.hibernate.test.criteria;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Student {
	private long studentNumber;
	private String name;
	private Set enrolments = new HashSet();
	public Set getEnrolments() {
		return enrolments;
	}
	public void setEnrolments(Set employments) {
		this.enrolments = employments;
	}
	public long getStudentNumber() {
		return studentNumber;
	}
	public void setStudentNumber(long studentNumber) {
		this.studentNumber = studentNumber;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
