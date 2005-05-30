/*
 * Created on 13/Nov/2003
 *  
 */
package net.sourceforge.fenixedu.applicationTier.Servico.publication;

import java.util.List;

import net.sourceforge.fenixedu.applicationTier.IServico;
import net.sourceforge.fenixedu.applicationTier.Servico.exceptions.FenixServiceException;
import net.sourceforge.fenixedu.dataTransferObject.util.Cloner;
import net.sourceforge.fenixedu.domain.publication.IPublicationFormat;
import net.sourceforge.fenixedu.domain.publication.PublicationFormat;
import net.sourceforge.fenixedu.persistenceTier.ExcepcaoPersistencia;
import net.sourceforge.fenixedu.persistenceTier.ISuportePersistente;
import net.sourceforge.fenixedu.persistenceTier.PersistenceSupportFactory;
import net.sourceforge.fenixedu.persistenceTier.publication.IPersistentPublicationFormat;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

/**
 * @author TJBF
 * @author PFON
 *  
 */
public class ReadPublicationFormats implements IServico {
    private static ReadPublicationFormats service = new ReadPublicationFormats();

    /**
     *  
     */
    private ReadPublicationFormats() {

    }

    public static ReadPublicationFormats getService() {

        return service;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ServidorAplicacao.IServico#getNome()
     */
    public String getNome() {
        return "ReadPublicationFormats";
    }

    public List run(String user, int publicationTypeId) throws FenixServiceException {
        try {
            ISuportePersistente persistentSuport = PersistenceSupportFactory.getDefaultPersistenceSupport();

            IPersistentPublicationFormat persistentPublicationFormat = persistentSuport
                    .getIPersistentPublicationFormat();

            List publicationFormatList = (List)persistentPublicationFormat.readAll(PublicationFormat.class);

            List result = (List) CollectionUtils.collect(publicationFormatList, new Transformer() {
                public Object transform(Object o) {
                    IPublicationFormat publicationFormat = (IPublicationFormat) o;
                    return Cloner.copyIPublicationFormat2InfoPublicationFormat(publicationFormat);
                }
            });

            return result;
        } catch (ExcepcaoPersistencia e) {
            throw new FenixServiceException(e);
        }
    }
}