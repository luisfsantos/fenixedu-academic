package net.sourceforge.fenixedu.domain.parking;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import net.sourceforge.fenixedu.dataTransferObject.parking.ParkingPartyBean;
import net.sourceforge.fenixedu.dataTransferObject.parking.VehicleBean;
import net.sourceforge.fenixedu.domain.Employee;
import net.sourceforge.fenixedu.domain.ExecutionSemester;
import net.sourceforge.fenixedu.domain.ExecutionYear;
import net.sourceforge.fenixedu.domain.PartyClassification;
import net.sourceforge.fenixedu.domain.Person;
import net.sourceforge.fenixedu.domain.RootDomainObject;
import net.sourceforge.fenixedu.domain.StudentCurricularPlan;
import net.sourceforge.fenixedu.domain.Teacher;
import net.sourceforge.fenixedu.domain.assiduousness.AssiduousnessStatusHistory;
import net.sourceforge.fenixedu.domain.degree.DegreeType;
import net.sourceforge.fenixedu.domain.exceptions.DomainException;
import net.sourceforge.fenixedu.domain.grant.contract.GrantContract;
import net.sourceforge.fenixedu.domain.grant.contract.GrantContractRegime;
import net.sourceforge.fenixedu.domain.grant.owner.GrantOwner;
import net.sourceforge.fenixedu.domain.organizationalStructure.AccountabilityTypeEnum;
import net.sourceforge.fenixedu.domain.organizationalStructure.Invitation;
import net.sourceforge.fenixedu.domain.organizationalStructure.Party;
import net.sourceforge.fenixedu.domain.organizationalStructure.Unit;
import net.sourceforge.fenixedu.domain.parking.ParkingRequest.ParkingRequestFactoryCreator;
import net.sourceforge.fenixedu.domain.person.RoleType;
import net.sourceforge.fenixedu.domain.student.Registration;
import net.sourceforge.fenixedu.domain.student.Student;
import net.sourceforge.fenixedu.domain.teacher.TeacherProfessionalSituation;
import pt.utl.ist.fenix.tools.util.i18n.Language;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import pt.utl.ist.fenix.tools.file.FileManagerFactory;

public class ParkingParty extends ParkingParty_Base {

    public static ParkingParty readByCardNumber(Long cardNumber) {
	for (ParkingParty parkingParty : RootDomainObject.getInstance().getParkingParties()) {
	    if (parkingParty.getCardNumber() != null && parkingParty.getCardNumber().equals(cardNumber)) {
		return parkingParty;
	    }
	}
	return null;
    }

    public ParkingParty(Party party) {
	super();
	setRootDomainObject(RootDomainObject.getInstance());
	setParty(party);
	setAuthorized(Boolean.FALSE);
	setAcceptedRegulation(Boolean.FALSE);
    }

    public boolean getHasAllNecessaryPersonalInfo() {
	return ((getParty().getDefaultPhone() != null && !StringUtils.isEmpty(getParty().getDefaultPhone().getNumber())) || (getParty()
		.getDefaultMobilePhone() != null && !StringUtils.isEmpty(getParty().getDefaultMobilePhone().getNumber())))
		&& (isEmployee() || (getParty().getDefaultEmailAddress() != null
			&& getParty().getDefaultEmailAddress().hasValue() && !StringUtils.isEmpty(getParty()
			.getDefaultEmailAddress().getValue())));
    }

    private boolean isEmployee() {
	if (getParty().isPerson()) {
	    Person person = (Person) getParty();
	    Teacher teacher = person.getTeacher();
	    if (teacher == null) {
		Employee employee = person.getEmployee();
		if (employee != null) {
		    return true;
		}
	    }
	}
	return false;
    }

    public List<ParkingRequest> getOrderedParkingRequests() {
	List<ParkingRequest> requests = new ArrayList<ParkingRequest>(getParkingRequests());
	Collections.sort(requests, new BeanComparator("creationDate"));
	return requests;
    }

    public ParkingRequest getFirstRequest() {
	List<ParkingRequest> requests = getOrderedParkingRequests();
	if (requests.size() != 0) {
	    return requests.get(0);
	}
	return null;
    }

    public ParkingRequest getLastRequest() {
	List<ParkingRequest> requests = getOrderedParkingRequests();
	if (requests.size() != 0) {
	    return requests.get(requests.size() - 1);
	}
	return null;
    }

    public ParkingRequestFactoryCreator getParkingRequestFactoryCreator() {
	return new ParkingRequestFactoryCreator(this);
    }

    public String getParkingAcceptedRegulationMessage() {
	ResourceBundle bundle = ResourceBundle.getBundle("resources.ParkingResources", Language.getLocale());
	String name = getParty().getName();
	String number = "";
	if (getParty().isPerson()) {
	    Person person = (Person) getParty();
	    Teacher teacher = person.getTeacher();
	    if (teacher == null) {
		Employee employee = person.getEmployee();
		if (employee == null) {
		    Student student = person.getStudent();
		    if (student != null) {
			number = student.getNumber().toString();
		    }
		} else {
		    number = employee.getEmployeeNumber().toString();
		}

	    } else {
		number = teacher.getTeacherNumber().toString();
	    }
	}

	return MessageFormat.format(bundle.getString("message.acceptedRegulation"), new Object[] { name, number });
    }

    public boolean isStudent() {
	if (getParty().isPerson()) {
	    Person person = (Person) getParty();
	    Teacher teacher = person.getTeacher();
	    if (teacher == null) {
		Employee employee = person.getEmployee();
		if (employee == null) {
		    Student student = person.getStudent();
		    if (student != null) {
			return true;
		    }
		}
	    }
	}
	return false;
    }

    public String getDriverLicenseFileNameToDisplay() {
	NewParkingDocument driverLicenseDocument = getDriverLicenseDocument();
	if (driverLicenseDocument != null) {
	    return driverLicenseDocument.getParkingFile().getFilename();
	} else if (getDriverLicenseDeliveryType() != null) {
	    ResourceBundle bundle = ResourceBundle.getBundle("resources.ParkingResources", Language.getLocale());
	    return bundle.getString(getDriverLicenseDeliveryType().name());
	}
	return "";
    }

    public String getDeclarationDocumentLink() {
	NewParkingDocument parkingDocument = getDriverLicenseDocument();
	if (parkingDocument != null && parkingDocument.getParkingDocumentType() == NewParkingDocumentType.DRIVER_LICENSE) {
	    ParkingFile parkingFile = parkingDocument.getParkingFile();
	    return FileManagerFactory.getFactoryInstance().getFileManager().formatDownloadUrl(
		    parkingFile.getExternalStorageIdentification(), parkingFile.getFilename());
	}
	return "";
    }

    public String getParkingGroupToDisplay() {
	if (getParkingGroup() != null) {
	    return getParkingGroup().getGroupName();
	}
	return null;
    }

    public String getWorkPhone() {
	if (getParty().isPerson()) {
	    return getParty().getDefaultPhone().getNumber();
	}
	return null;
    }

    public List<RoleType> getSubmitAsRoles() {
	List<RoleType> roles = new ArrayList<RoleType>();
	if (getParty().isPerson()) {
	    Person person = (Person) getParty();
	    Teacher teacher = person.getTeacher();
	    if (teacher != null && person.getPersonRole(RoleType.TEACHER) != null
		    && !teacher.isMonitor(ExecutionSemester.readActualExecutionSemester())) {
		roles.add(RoleType.TEACHER);
	    }
	    Employee employee = person.getEmployee();
	    if (employee != null && person.getPersonRole(RoleType.TEACHER) == null
		    && person.getPersonRole(RoleType.EMPLOYEE) != null
		    && employee.getCurrentContractByContractType(AccountabilityTypeEnum.WORKING_CONTRACT) != null) {
		roles.add(RoleType.EMPLOYEE);
	    }
	    Student student = person.getStudent();
	    if (student != null && person.getPersonRole(RoleType.STUDENT) != null) {
		DegreeType degreeType = student.getMostSignificantDegreeType();
		Collection<Registration> registrations = student.getRegistrationsByDegreeType(degreeType);
		for (Registration registration : registrations) {
		    StudentCurricularPlan scp = registration.getActiveStudentCurricularPlan();
		    if (scp != null) {
			roles.add(RoleType.STUDENT);
			break;
		    }
		}
	    }
	    GrantOwner grantOwner = person.getGrantOwner();
	    if (grantOwner != null && person.getPersonRole(RoleType.GRANT_OWNER) != null && grantOwner.hasCurrentContract()) {
		roles.add(RoleType.GRANT_OWNER);
	    }
	}
	if (roles.size() == 0) {
	    roles.add(RoleType.PERSON);
	}
	return roles;
    }

    public List<String> getOccupations() {
	List<String> occupations = new ArrayList<String>();
	if (getParty().isPerson()) {
	    Person person = (Person) getParty();
	    Teacher teacher = person.getTeacher();
	    if (teacher != null) {
		StringBuilder stringBuilder = new StringBuilder();
		String currentDepartment = "";
		if (teacher.getCurrentWorkingDepartment() != null) {
		    currentDepartment = teacher.getCurrentWorkingDepartment().getName();
		}
		if (teacher.isMonitor(ExecutionSemester.readActualExecutionSemester())) {
		    stringBuilder.append("<strong>Monitor</strong><br/> N� " + teacher.getTeacherNumber() + "<br/>"
			    + currentDepartment);
		} else {
		    stringBuilder.append("<strong>Docente</strong><br/> N� " + teacher.getTeacherNumber() + "<br/>"
			    + currentDepartment);
		}
		TeacherProfessionalSituation teacherProfessionalSituation = teacher
			.getCurrentLegalRegimenWithoutSpecialSitutions();
		if (teacherProfessionalSituation == null) {
		    teacherProfessionalSituation = teacher.getLastLegalRegimenWithoutSpecialSituations();
		}
		if (teacherProfessionalSituation != null) {
		    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy/MM/dd");
		    stringBuilder.append("\n (Data inicio: ").append(
			    fmt.print(teacherProfessionalSituation.getBeginDateYearMonthDay()));
		    if (teacherProfessionalSituation.getEndDateYearMonthDay() != null) {
			stringBuilder.append(" - Data fim: ").append(
				fmt.print(teacherProfessionalSituation.getEndDateYearMonthDay()));
		    }
		    stringBuilder.append(")<br/>");
		} else {
		    stringBuilder.append("(inactivo)<br/>");
		}
		occupations.add(stringBuilder.toString());
	    }
	    Employee employee = person.getEmployee();
	    if (employee != null && person.getPersonRole(RoleType.TEACHER) == null
		    && person.getPersonRole(RoleType.EMPLOYEE) != null
		    && employee.getCurrentContractByContractType(AccountabilityTypeEnum.WORKING_CONTRACT) != null
		    && !person.isPersonResearcher()) {
		StringBuilder stringBuilder = new StringBuilder();
		Unit currentUnit = employee.getCurrentWorkingPlace();
		if (currentUnit != null) {
		    stringBuilder.append("<strong>Funcion�rio</strong><br/> N� " + employee.getEmployeeNumber() + "<br/>"
			    + currentUnit.getName() + " - " + currentUnit.getCostCenterCode());
		} else {
		    stringBuilder.append("<strong>Funcion�rio</strong><br/> N� " + employee.getEmployeeNumber());
		}
		if (employee.getAssiduousness() != null) {
		    AssiduousnessStatusHistory assiduousnessStatusHistory = employee.getAssiduousness()
			    .getCurrentOrLastAssiduousnessStatusHistory();
		    if (assiduousnessStatusHistory != null) {
			DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy/MM/dd");
			stringBuilder.append("<br/> (Data inicio: ").append(fmt.print(assiduousnessStatusHistory.getBeginDate()));
			if (assiduousnessStatusHistory.getEndDate() != null) {
			    stringBuilder.append(" - Data fim: ").append(fmt.print(assiduousnessStatusHistory.getEndDate()));
			}
			stringBuilder.append(")<br/>");
		    }
		}
		occupations.add(stringBuilder.toString());
	    }
	    GrantOwner grantOwner = person.getGrantOwner();
	    if (grantOwner != null && person.getPersonRole(RoleType.GRANT_OWNER) != null && grantOwner.hasCurrentContract()) {
		List<GrantContractRegime> contractRegimeList = new ArrayList<GrantContractRegime>();
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<strong>Bolseiro</strong><br/> N� " + grantOwner.getNumber());
		for (GrantContract contract : grantOwner.getGrantContracts()) {
		    contractRegimeList.addAll(contract.getContractRegimes());
		}
		Collections.sort(contractRegimeList, new BeanComparator("dateBeginContractYearMonthDay"));
		for (GrantContractRegime contractRegime : contractRegimeList) {

		    stringBuilder.append("<br/><strong>In�cio:</strong> "
			    + contractRegime.getDateBeginContractYearMonthDay().toString("dd/MM/yyyy"));
		    stringBuilder.append("&nbsp&nbsp&nbsp -&nbsp&nbsp&nbsp<strong>Fim:</strong> "
			    + contractRegime.getDateEndContractYearMonthDay().toString("dd/MM/yyyy"));
		    stringBuilder.append("&nbsp&nbsp&nbsp -&nbsp&nbsp&nbsp<strong>Activo:</strong> ");
		    if (contractRegime.isActive()) {
			stringBuilder.append("Sim");
		    } else {
			stringBuilder.append("N�o");
		    }
		}
		occupations.add(stringBuilder.toString());
	    }
	    if (person.isPersonResearcher()) {
		String researchUnitNames = person.getWorkingResearchUnitNames();
		if (!StringUtils.isEmpty(researchUnitNames)
			|| !person.getPartyClassification().equals(PartyClassification.TEACHER)) {
		    occupations.add("<strong>Investigador</strong><br/> N� " + person.getMostSignificantNumber());
		    if (!StringUtils.isEmpty(researchUnitNames)) {
			occupations.add("<br/>" + researchUnitNames);
		    }
		}
	    }
	    Student student = person.getStudent();
	    if (student != null && person.getPersonRole(RoleType.STUDENT) != null) {

		StringBuilder stringBuilder = null;
		for (Registration registration : student.getActiveRegistrations()) {
		    StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
		    if (scp != null) {
			if (stringBuilder == null) {
			    stringBuilder = new StringBuilder("<strong>Estudante</strong><br/> N� ");
			    stringBuilder.append(student.getNumber()).append("<br/>");
			}
			stringBuilder.append("\n").append(scp.getDegreeCurricularPlan().getName());
			stringBuilder.append("\n (").append(registration.getCurricularYear()).append("� ano");
			if (isFirstTimeEnrolledInCurrentYear(registration)) {
			    stringBuilder.append(" - 1� vez)");
			} else {
			    stringBuilder.append(")");
			}
			stringBuilder.append("<br/>M�dia: ").append(registration.getAverage());
			stringBuilder.append("<br/>");
		    }
		}
		if (stringBuilder != null) {
		    occupations.add(stringBuilder.toString());
		}
	    }
	    List<Invitation> invitations = person.getActiveInvitations();
	    if (!invitations.isEmpty()) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<strong>Convidado</strong><br/>");
		for (Invitation invitation : invitations) {
		    stringBuilder.append("<strong>Por:</strong> ").append(invitation.getUnit().getName()).append("<br/>");
		    stringBuilder.append("<strong>In�cio:</strong> " + invitation.getBeginDate().toString("dd/MM/yyyy"));
		    stringBuilder.append("&nbsp&nbsp&nbsp -&nbsp&nbsp&nbsp<strong>Fim:</strong> "
			    + invitation.getEndDate().toString("dd/MM/yyyy"));
		    occupations.add(stringBuilder.toString());
		}
	    }
	}
	return occupations;
    }

    public List<String> getPastOccupations() {
	List<String> occupations = new ArrayList<String>();
	if (getParty().isPerson()) {
	    Person person = (Person) getParty();
	    Teacher teacher = person.getTeacher();
	    if (teacher != null) {
		StringBuilder stringBuilder = new StringBuilder();
		String currentDepartment = "";
		if (teacher.getCurrentWorkingDepartment() != null) {
		    currentDepartment = teacher.getCurrentWorkingDepartment().getName();
		}
		if (teacher.isMonitor(ExecutionSemester.readActualExecutionSemester())) {
		    stringBuilder.append("<strong>Monitor</strong><br/> N� " + teacher.getTeacherNumber() + "<br/>"
			    + currentDepartment);
		} else {
		    stringBuilder.append("<strong>Docente</strong><br/> N� " + teacher.getTeacherNumber() + "<br/>"
			    + currentDepartment);
		}
		TeacherProfessionalSituation teacherProfessionalSituation = teacher
			.getCurrentLegalRegimenWithoutSpecialSitutions();
		if (teacherProfessionalSituation == null) {
		    teacherProfessionalSituation = teacher.getLastLegalRegimenWithoutSpecialSituations();
		}
		if (teacherProfessionalSituation != null) {
		    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy/MM/dd");
		    stringBuilder.append("\n (Data inicio: ").append(
			    fmt.print(teacherProfessionalSituation.getBeginDateYearMonthDay()));
		    if (teacherProfessionalSituation.getEndDateYearMonthDay() != null) {
			stringBuilder.append(" - Data fim: ").append(
				fmt.print(teacherProfessionalSituation.getEndDateYearMonthDay()));
		    }
		    stringBuilder.append(")<br/>");
		} else {
		    stringBuilder.append("(inactivo)<br/>");
		}
		occupations.add(stringBuilder.toString());
	    }
	    Employee employee = person.getEmployee();
	    if (employee != null && person.getPersonRole(RoleType.TEACHER) == null
		    && employee.getCurrentContractByContractType(AccountabilityTypeEnum.WORKING_CONTRACT) != null
		    && !person.isPersonResearcher()) {
		StringBuilder stringBuilder = new StringBuilder();
		AssiduousnessStatusHistory assiduousnessStatusHistory = employee.getAssiduousness()
			.getCurrentOrLastAssiduousnessStatusHistory();
		if (assiduousnessStatusHistory != null) {
		    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy/MM/dd");
		    stringBuilder.append("<br/> (Data inicio: ").append(fmt.print(assiduousnessStatusHistory.getBeginDate()));
		    if (assiduousnessStatusHistory.getEndDate() != null) {
			stringBuilder.append(" - Data fim: ").append(fmt.print(assiduousnessStatusHistory.getEndDate()));
		    }
		    stringBuilder.append(")<br/>");
		}
		occupations.add(stringBuilder.toString());
	    }
	    Student student = person.getStudent();
	    if (student != null) {

		StringBuilder stringBuilder = null;
		for (Registration registration : student.getRegistrations()) {
		    StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
		    if (scp != null) {
			if (stringBuilder == null) {
			    stringBuilder = new StringBuilder("<strong>Estudante</strong><br/> N� ");
			    stringBuilder.append(student.getNumber()).append("<br/>");
			}
			stringBuilder.append("\n").append(scp.getDegreeCurricularPlan().getName());
			stringBuilder.append("\n (").append(registration.getCurricularYear()).append("� ano");
			if (isFirstTimeEnrolledInCurrentYear(registration)) {
			    stringBuilder.append(" - 1� vez)");
			} else {
			    stringBuilder.append(")");
			}
			stringBuilder.append("<br/>M�dia: ").append(registration.getAverage());
			stringBuilder.append("<br/>");
		    }
		}
		if (stringBuilder != null) {
		    occupations.add(stringBuilder.toString());
		}
	    }
	    if (person.isPersonResearcher()) {
		String researchUnitNames = person.getWorkingResearchUnitNames();
		if (!StringUtils.isEmpty(researchUnitNames)
			|| !person.getPartyClassification().equals(PartyClassification.TEACHER)) {
		    occupations.add("<strong>Investigador</strong><br/> N� " + person.getMostSignificantNumber());
		    if (!StringUtils.isEmpty(researchUnitNames)) {
			occupations.add("<br/>" + researchUnitNames);
		    }
		}
	    }
	    GrantOwner grantOwner = person.getGrantOwner();
	    if (grantOwner != null) {
		List<GrantContractRegime> contractRegimeList = new ArrayList<GrantContractRegime>();
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<strong>Bolseiro</strong><br/> N� " + grantOwner.getNumber());
		for (GrantContract contract : grantOwner.getGrantContracts()) {
		    contractRegimeList.addAll(contract.getContractRegimes());
		}
		Collections.sort(contractRegimeList, new BeanComparator("dateBeginContractYearMonthDay"));
		for (GrantContractRegime contractRegime : contractRegimeList) {
		    stringBuilder.append("<br/><strong>In�cio:</strong> "
			    + contractRegime.getDateBeginContractYearMonthDay().toString("dd/MM/yyyy"));
		    stringBuilder.append("&nbsp&nbsp&nbsp -&nbsp&nbsp&nbsp<strong>Fim:</strong> "
			    + contractRegime.getDateEndContractYearMonthDay().toString("dd/MM/yyyy"));
		    stringBuilder.append("&nbsp&nbsp&nbsp -&nbsp&nbsp&nbsp<strong>Activo:</strong> ");
		    if (contractRegime.isActive()) {
			stringBuilder.append("Sim");
		    } else {
			stringBuilder.append("N�o");
		    }
		}
		occupations.add(stringBuilder.toString());
	    }
	    List<Invitation> invitations = person.getActiveInvitations();
	    if (!invitations.isEmpty()) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<strong>Convidado</strong><br/>");
		for (Invitation invitation : invitations) {
		    stringBuilder.append("<strong>Por:</strong> ").append(invitation.getUnit().getName()).append("<br/>");
		    stringBuilder.append("<strong>In�cio:</strong> " + invitation.getBeginDate().toString("dd/MM/yyyy"));
		    stringBuilder.append("&nbsp&nbsp&nbsp -&nbsp&nbsp&nbsp<strong>Fim:</strong> "
			    + invitation.getEndDate().toString("dd/MM/yyyy"));
		    occupations.add(stringBuilder.toString());
		}
	    }
	}
	return occupations;
    }

    public List<String> getDegreesInformation() {
	List<String> result = new ArrayList<String>();
	if (getParty().isPerson()) {
	    Person person = (Person) getParty();
	    Student student = person.getStudent();
	    if (student != null && person.getPersonRole(RoleType.STUDENT) != null) {
		for (Registration registration : student.getActiveRegistrations()) {
		    StringBuilder stringBuilder = new StringBuilder();
		    StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
		    if (scp != null) {
			stringBuilder.append(scp.getDegreeCurricularPlan().getName());
			stringBuilder.append(" ").append(registration.getCurricularYear()).append("� ano");
			if (isFirstTimeEnrolledInCurrentYear(registration)) {
			    stringBuilder.append(" - 1� vez");
			}
			stringBuilder.append(" - ").append(registration.getAverage());
			result.add(stringBuilder.toString());
		    }
		}
	    }
	}
	return result;
    }

    public boolean hasVehicleContainingPlateNumber(String plateNumber) {
	String plateNumberLowerCase = plateNumber.toLowerCase();
	for (Vehicle vehicle : getVehicles()) {
	    if (vehicle.getPlateNumber().toLowerCase().contains(plateNumberLowerCase)) {
		return true;
	    }
	}
	return false;
    }

    public void delete() {
	if (canBeDeleted()) {
	    removeParty();
	    removeParkingGroup();
	    deleteDriverLicenseDocument();
	    for (; getVehicles().size() != 0; getVehicles().get(0).delete())
		;
	    for (; getParkingRequests().size() != 0; getParkingRequests().get(0).delete())
		;
	    removeRootDomainObject();
	    deleteDomainObject();
	}
    }

    private void deleteDriverLicenseDocument() {
	NewParkingDocument parkingDocument = getDriverLicenseDocument();
	if (parkingDocument != null) {
	    parkingDocument.delete();
	}
    }

    private boolean canBeDeleted() {
	return getVehicles().isEmpty();
    }

    public boolean hasFirstTimeRequest() {
	return getFirstRequest() != null;
    }

    public Integer getMostSignificantNumber() {
	if (getParty().isPerson()) {
	    if (getPhdNumber() != null) {
		return getPhdNumber();
	    }
	    Person person = (Person) getParty();
	    if (person.getTeacher() != null && person.getTeacher().getCurrentWorkingDepartment() != null
		    && !person.getTeacher().isMonitor(ExecutionSemester.readActualExecutionSemester())) {
		return person.getTeacher().getTeacherNumber();
	    }
	    if (person.getEmployee() != null && person.getEmployee().getCurrentWorkingContract() != null
		    && person.getPersonRole(RoleType.TEACHER) == null) {
		return person.getEmployee().getEmployeeNumber();
	    }
	    if (getPartyClassification().equals(PartyClassification.RESEARCHER) && person.getEmployee() != null) {
		return person.getEmployee().getEmployeeNumber();
	    }
	    if (person.getStudent() != null) {
		DegreeType degreeType = person.getStudent().getMostSignificantDegreeType();
		Collection<Registration> registrations = person.getStudent().getRegistrationsByDegreeType(degreeType);
		for (Registration registration : registrations) {
		    StudentCurricularPlan scp = registration.getActiveStudentCurricularPlan();
		    if (scp != null) {
			return person.getStudent().getNumber();
		    }
		}
	    }
	    if (person.getGrantOwner() != null && person.getGrantOwner().hasCurrentContract()) {
		return person.getGrantOwner().getNumber();
	    }
	    if (person.getTeacher() != null && person.getTeacher().getCurrentWorkingDepartment() != null
		    && person.getTeacher().isMonitor(ExecutionSemester.readActualExecutionSemester())) {
		return person.getTeacher().getTeacherNumber();
	    }
	}
	return 0;
    }

    public static List<ParkingParty> getAll() {
	return RootDomainObject.getInstance().getParkingParties();
    }

    public void edit(ParkingPartyBean parkingPartyBean) {
	if (!parkingPartyBean.getCardAlwaysValid()
		&& parkingPartyBean.getCardStartDate().isAfter(parkingPartyBean.getCardEndDate())) {
	    throw new DomainException("error.parkingParty.invalidPeriod");
	}
	if (getCardNumber() != null
		&& (changedDates(getCardStartDate(), parkingPartyBean.getCardStartDate(), parkingPartyBean.getCardAlwaysValid())
			|| changedDates(getCardEndDate(), parkingPartyBean.getCardEndDate(), parkingPartyBean
				.getCardAlwaysValid()) || changedObject(getCardNumber(), parkingPartyBean.getCardNumber())
			|| changedObject(getParkingGroup(), parkingPartyBean.getParkingGroup()) || changedObject(getPhdNumber(),
			parkingPartyBean.getPhdNumber()))) {
	    new ParkingPartyHistory(this, false);
	}
	setCardNumber(parkingPartyBean.getCardNumber());
	setCardStartDate(parkingPartyBean.getCardStartDate());
	setCardEndDate(parkingPartyBean.getCardEndDate());
	setPhdNumber(parkingPartyBean.getPhdNumber());
	setParkingGroup(parkingPartyBean.getParkingGroup());
	for (VehicleBean vehicleBean : parkingPartyBean.getVehicles()) {
	    if (vehicleBean.getVehicle() != null) {
		if (vehicleBean.getDeleteVehicle()) {
		    vehicleBean.getVehicle().delete();
		} else {
		    vehicleBean.getVehicle().edit(vehicleBean);
		}
	    } else {
		if (!vehicleBean.getDeleteVehicle()) {
		    new Vehicle(vehicleBean);
		}
	    }
	}
	setNotes(parkingPartyBean.getNotes());
    }

    private boolean changedDates(DateTime oldDate, DateTime newDate, Boolean cardAlwaysValid) {
	return cardAlwaysValid ? (oldDate == null ? false : true) : ((oldDate == null || (!oldDate.equals(newDate))) ? true
		: oldDate.equals(newDate));
    }

    private boolean changedObject(Object oldObject, Object newObject) {
	return oldObject == null && newObject == null ? false : (oldObject != null && newObject != null ? (!oldObject
		.equals(newObject)) : true);
    }

    public void edit(ParkingRequest parkingRequest) {
	setDriverLicenseDeliveryType(parkingRequest.getDriverLicenseDeliveryType());
	parkingRequest.deleteDriverLicenseDocument();

	for (Vehicle vehicle : parkingRequest.getVehicles()) {
	    Vehicle partyVehicle = geVehicleByPlateNumber(vehicle.getPlateNumber());
	    if (partyVehicle != null) {
		partyVehicle.edit(vehicle);
		vehicle.deleteDocuments();
	    } else {
		addVehicles(new Vehicle(vehicle));
		vehicle.deleteDocuments();
	    }
	}
	setRequestedAs(parkingRequest.getRequestedAs());
    }

    private Vehicle geVehicleByPlateNumber(String plateNumber) {
	for (Vehicle vehicle : getVehicles()) {
	    if (vehicle.getPlateNumber().equalsIgnoreCase(plateNumber)) {
		return vehicle;
	    }
	}
	return null;
    }

    public boolean isActiveInHisGroup() {
	if (getParkingGroup() == null) {
	    return Boolean.FALSE;
	}
	if (getParty().isPerson()) {
	    Person person = (Person) getParty();
	    if (getParkingGroup().getGroupName().equalsIgnoreCase("Docentes")) {
		return person.getTeacher() != null && person.getTeacher().getCurrentWorkingDepartment() != null;
	    }
	    if (getParkingGroup().getGroupName().equalsIgnoreCase("N�o Docentes")) {
		return person.getEmployee() != null && person.getEmployee().getCurrentWorkingPlace() != null;
	    }
	    if (getParkingGroup().getGroupName().equalsIgnoreCase("Especiais")) {
		return person.getPartyClassification() != PartyClassification.PERSON;
	    }
	    if (getParkingGroup().getGroupName().equalsIgnoreCase("2� ciclo")) {
		if (person.hasStudent()) {
		    return canRequestUnlimitedCard(person.getStudent());
		} else {
		    return Boolean.FALSE;
		}
	    }
	    if (getParkingGroup().getGroupName().equalsIgnoreCase("Bolseiros")) {
		return person.hasGrantOwner() && person.getGrantOwner().hasCurrentContract();
	    }
	    if (getParkingGroup().getGroupName().equalsIgnoreCase("3� ciclo")) {
		if (person.hasStudent()) {
		    Registration registration = getRegistrationByDegreeType(person.getStudent(), DegreeType.BOLONHA_PHD_PROGRAM);
		    return registration != null && registration.isActive();
		} else {
		    return Boolean.FALSE;
		}
	    }
	    if (getParkingGroup().getGroupName().equalsIgnoreCase("Limitados")) {
		return person.getPartyClassification() != PartyClassification.PERSON
			&& person.getPartyClassification() != PartyClassification.RESEARCHER;
	    }
	}
	return Boolean.FALSE;
    }

    public boolean getCanRequestUnlimitedCardAndIsInAnyRequestPeriod() {
	ParkingRequestPeriod current = ParkingRequestPeriod.getCurrentRequestPeriod();
	return current != null && canRequestUnlimitedCard(current);
    }

    public boolean canRequestUnlimitedCard() {
	return canRequestUnlimitedCard(ParkingRequestPeriod.getCurrentRequestPeriod());
    }

    public boolean canRequestUnlimitedCard(ParkingRequestPeriod parkingRequestPeriod) {
	List<RoleType> roles = getSubmitAsRoles();
	if (!alreadyRequestParkingRequestTypeInPeriod(ParkingRequestType.RENEW, parkingRequestPeriod)) {
	    if (roles.contains(RoleType.GRANT_OWNER)) {
		return Boolean.TRUE;
	    } else if (roles.contains(RoleType.STUDENT) && canRequestUnlimitedCard(((Person) getParty()).getStudent())) {
		return Boolean.TRUE;
	    }
	}
	return Boolean.FALSE;
    }

    public boolean alreadyRequestParkingRequestTypeInPeriod(ParkingRequestType parkingRequestType,
	    ParkingRequestPeriod parkingRequestPeriod) {
	List<ParkingRequest> requests = getOrderedParkingRequests();
	for (ParkingRequest parkingRequest : requests) {
	    if (parkingRequestPeriod.getRequestPeriodInterval().contains(parkingRequest.getCreationDate())
		    && parkingRequest.getParkingRequestType().equals(parkingRequestType)) {
		return true;
	    }
	}
	return false;
    }

    public boolean canRequestUnlimitedCard(Student student) {
	List<DegreeType> degreeTypes = new ArrayList<DegreeType>();
	degreeTypes.add(DegreeType.DEGREE);
	degreeTypes.add(DegreeType.BOLONHA_ADVANCED_FORMATION_DIPLOMA);
	degreeTypes.add(DegreeType.BOLONHA_MASTER_DEGREE);
	degreeTypes.add(DegreeType.BOLONHA_INTEGRATED_MASTER_DEGREE);

	for (DegreeType degreeType : degreeTypes) {
	    Registration registration = getRegistrationByDegreeType(student, degreeType);
	    if (registration != null && registration.isInFinalDegreeYear()) {
		return degreeType.equals(DegreeType.BOLONHA_ADVANCED_FORMATION_DIPLOMA) ? Boolean.TRUE
			: isFirstTimeEnrolledInCurrentYear(registration);
	    }
	}
	return false;
	// DEGREE=Licenciatura (5 anos) - 5� ano
	// MASTER_DEGREE=Mestrado = 2ciclo - n�o tem
	// BOLONHA_DEGREE=Licenciatura Bolonha - n�o podem
	// BOLONHA_MASTER_DEGREE=Mestrado Bolonha - s� no 5+ ano 1� vez
	// BOLONHA_INTEGRATED_MASTER_DEGREE=Mestrado Integrado
	// BOLONHA_ADVANCED_FORMATION_DIPLOMA =Diploma Forma��o Avan�ada = cota
	// pos grad

	// BOLONHA_PHD_PROGRAM=Programa Doutoral - n�o est�o no f�nix
	// BOLONHA_SPECIALIZATION_DEGREE=Curso de Especializa��o - n�o est�o no
	// f�nix

    }

    public boolean isFirstTimeEnrolledInCurrentYear(Registration registration) {
	ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
	return registration.getCurricularYear(executionYear.getPreviousExecutionYear()) != registration
		.getCurricularYear(executionYear);
    }

    private Registration getRegistrationByDegreeType(Student student, DegreeType degreeType) {
	for (Registration registration : student.getRegistrationsByDegreeType(degreeType)) {
	    if (registration.isActive()) {
		StudentCurricularPlan scp = registration.getActiveStudentCurricularPlan();
		if (scp != null) {
		    return registration;
		}
	    }
	}
	return null;
    }

    public PartyClassification getPartyClassification() {
	return getParty().isPerson() ? ((Person) getParty()).getPartyClassification() : null;
    }

    public DateTime getCardEndDateToCompare() {
	if (getCardEndDate() == null) {
	    return new DateTime(9999, 9, 9, 9, 9, 9, 9);
	} else {
	    return getCardEndDate();
	}
    }

    public DateTime getCardStartDateToCompare() {
	if (getCardStartDate() == null) {
	    return new DateTime(9999, 9, 9, 9, 9, 9, 9);
	} else {
	    return getCardStartDate();
	}
    }

    public void renewParkingCard(DateTime newEndDate, ParkingGroup newParkingGroup) {
	setCardEndDate(newEndDate);
	if (newParkingGroup != null) {
	    setParkingGroup(newParkingGroup);
	}
    }
}
