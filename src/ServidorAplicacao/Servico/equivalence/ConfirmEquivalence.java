package ServidorAplicacao.Servico.equivalence;

import java.util.Date;
import java.util.Iterator;

import DataBeans.InfoCurricularCourseScope;
import DataBeans.InfoExecutionPeriod;
import DataBeans.InfoStudentCurricularPlan;
import DataBeans.equivalence.InfoCurricularCourseScopeGrade;
import DataBeans.equivalence.InfoEquivalenceContext;
import DataBeans.util.Cloner;
import Dominio.Enrolment;
import Dominio.EnrolmentEquivalence;
import Dominio.EnrolmentEvaluation;
import Dominio.Funcionario;
import Dominio.ICurricularCourseScope;
import Dominio.IEnrolment;
import Dominio.IEnrolmentEquivalence;
import Dominio.IEnrolmentEvaluation;
import Dominio.IExecutionPeriod;
import Dominio.IPessoa;
import Dominio.IStudentCurricularPlan;
import ServidorAplicacao.IServico;
import ServidorAplicacao.IUserView;
import ServidorAplicacao.Servico.exceptions.FenixServiceException;
import ServidorPersistente.ExcepcaoPersistencia;
import ServidorPersistente.IPersistentEnrolment;
import ServidorPersistente.IPersistentEnrolmentEquivalence;
import ServidorPersistente.IPersistentEnrolmentEvaluation;
import ServidorPersistente.IPessoaPersistente;
import ServidorPersistente.ISuportePersistente;
import ServidorPersistente.OJB.SuportePersistenteOJB;
import ServidorPersistenteJDBC.IFuncionarioPersistente;
import ServidorPersistenteJDBC.SuportePersistente;
import Util.EnrolmentEvaluationState;
import Util.EnrolmentEvaluationType;
import Util.EnrolmentState;
/**
 * @author David Santos
 * 9/Jul/2003
 */

public class ConfirmEquivalence implements IServico {

	private static ConfirmEquivalence service = new ConfirmEquivalence();

	public static ConfirmEquivalence getService() {
		return ConfirmEquivalence.service;
	}

	private ConfirmEquivalence() {
	}

	public final String getNome() {
		return "ConfirmEquivalence";
	}

	public InfoEquivalenceContext run(InfoEquivalenceContext infoEquivalenceContext) throws FenixServiceException {

		try {
			ISuportePersistente persistentSupport = SuportePersistenteOJB.getInstance();
			IPersistentEnrolment persistentEnrolment = persistentSupport.getIPersistentEnrolment();
			IPersistentEnrolmentEquivalence persistentEnrolmentEquivalence = persistentSupport.getIPersistentEnrolmentEquivalence();
			IPersistentEnrolmentEvaluation persistentEnrolmentEvaluation = persistentSupport.getIPersistentEnrolmentEvaluation();
			IPessoaPersistente pessoaPersistente = persistentSupport.getIPessoaPersistente();
			
			IFuncionarioPersistente funcionarioPersistente = SuportePersistente.getInstance().iFuncionarioPersistente();

			//			IPersistentEnrolmentEquivalence persistentEnrolmentEquivalence = persistentSupport.getIPersistentEnrolmentEq();

			Iterator scopesToGetEquivalenceIterator = infoEquivalenceContext.getChosenInfoCurricularCourseScopesToGetEquivalenceWithGrade().iterator();
			while (scopesToGetEquivalenceIterator.hasNext()) {
				InfoCurricularCourseScopeGrade infoCurricularCourseScopeGrade = (InfoCurricularCourseScopeGrade) scopesToGetEquivalenceIterator.next();

				//creates new Enrolment
				InfoCurricularCourseScope infoCurricularCourseScopeToEnrol = infoCurricularCourseScopeGrade.getInfoCurricularCourseScope();
				ICurricularCourseScope curricularCourseScopeToEnrol =
					Cloner.copyInfoCurricularCourseScope2ICurricularCourseScope(infoCurricularCourseScopeToEnrol);

				InfoExecutionPeriod infoExecutionPeriod = infoEquivalenceContext.getCurrentInfoExecutionPeriod();
				IExecutionPeriod executionPeriod = Cloner.copyInfoExecutionPeriod2IExecutionPeriod(infoExecutionPeriod);

				InfoStudentCurricularPlan infoStudentCurricularPlan = infoEquivalenceContext.getInfoStudentCurricularPlan();
				IStudentCurricularPlan studentCurricularPlan = Cloner.copyInfoStudentCurricularPlan2IStudentCurricularPlan(infoStudentCurricularPlan);

				IEnrolment newEnrolment = new Enrolment();
				newEnrolment.setCurricularCourseScope(curricularCourseScopeToEnrol);
				newEnrolment.setEnrolmentEvaluationType(EnrolmentEvaluationType.EQUIVALENCE_OBJ);
				newEnrolment.setEnrolmentState(EnrolmentState.APROVED);
				newEnrolment.setExecutionPeriod(executionPeriod);
				newEnrolment.setStudentCurricularPlan(studentCurricularPlan);
				persistentEnrolment.lockWrite(newEnrolment);

				//	creates new Enrolment Evaluation
				IUserView userView = infoEquivalenceContext.getResponsible();
				IPessoa pessoa = pessoaPersistente.lerPessoaPorUsername(userView.getUtilizador());
				Funcionario funcionario = funcionarioPersistente.lerFuncionarioPorPessoa(pessoa.getIdInternal().intValue());
							
				IEnrolmentEvaluation enrolmentEvaluation = new EnrolmentEvaluation();
				enrolmentEvaluation.setEnrolment(newEnrolment);
				enrolmentEvaluation.setEnrolmentEvaluationState(EnrolmentEvaluationState.FINAL_OBJ);
				enrolmentEvaluation.setEnrolmentEvaluationType(EnrolmentEvaluationType.EQUIVALENCE_OBJ);
				enrolmentEvaluation.setGrade(infoCurricularCourseScopeGrade.getGrade());
				enrolmentEvaluation.setPersonResponsibleForGrade(pessoa);
				enrolmentEvaluation.setEmployee(funcionario);
				enrolmentEvaluation.setWhen(new Date());
				// TODO DAVID-RICARDO: Quando o algoritmo do checksum estiver feito tem de ser actualizar este campo
				enrolmentEvaluation.setCheckSum(null);
				persistentEnrolmentEvaluation.lockWrite(enrolmentEvaluation);
				
				//	creates new Enrolment Equivalence
				IEnrolmentEquivalence enrolmentEquivalence = new EnrolmentEquivalence();
				enrolmentEquivalence.setEnrolment(newEnrolment);
				persistentEnrolmentEquivalence.lockWrite(enrolmentEquivalence);
				
				//				Iterator enrolmentsToGiveEquivalenceIterator = infoEquivalenceContext.getChosenInfoEnrolmentsToGiveEquivalence().iterator();
				//				while (enrolmentsToGiveEquivalenceIterator.hasNext()) {
				//					IEnrolment enrolmentToGiveEquivalence = (IEnrolment) enrolmentsToGiveEquivalenceIterator.next();
				//					IEnrolmentEquivalence enrolmentEquivalence = new EnrolmentEquivalence();
				//					enrolmentEquivalence.setEnrolment(newEnrolment);
				//					
				//				}
				//				TODO DAVID-RICARDO: Tratar op��es
			}

		} catch (ExcepcaoPersistencia e) {
			throw new FenixServiceException(e);
		}

		return infoEquivalenceContext;
	}
}