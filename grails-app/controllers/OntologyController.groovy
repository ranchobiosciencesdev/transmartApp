import annotation.AmTagItem
import annotation.AmTagTemplate
import com.recomdata.transmart.domain.i2b2.ExtData
import com.recomdata.transmart.domain.i2b2.ExtDataType
import fm.FmFolder
import fm.FmFolderAssociation
import grails.converters.JSON
import i2b2.OntNodeTag
import org.transmart.biomart.Experiment
import org.transmart.searchapp.AuthUser
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.StudiesResource
import org.transmartproject.core.ontology.Study
import org.transmartproject.db.concept.ConceptKey

class OntologyController {
    def dataSource;
    def index = {}
    def i2b2HelperService
    def springSecurityService
    def ontologyService
    def amTagTemplateService
    def amTagItemService
    ConceptsResource conceptsResourceService
    StudiesResource studiesResourceService
    def externalDataService

    def showOntTagFilter = {
        def tagtypesc = []
        tagtypesc.add("ALL")
        def tagtypes = i2b2.OntNodeTag.executeQuery("SELECT DISTINCT o.tagtype FROM i2b2.OntNodeTag as o order by o.tagtype")
        tagtypesc.addAll(tagtypes)
        def tags = i2b2.OntNodeTag.executeQuery("SELECT DISTINCT o.tag FROM i2b2.OntNodeTag o order by o.tag")
        /*WHERE o.tagtype='"+tagtypesc[0]+"'*/
        log.trace "${tags as JSON}"
        render(template: 'filter', model: [tagtypes: tagtypesc, tags: tags])
    }

    def ajaxGetOntTagFilterTerms = {
        def tagtype = params.tagtype
        log.trace("calling search for tagtype:" + tagtype)
        def tags = i2b2.OntNodeTag.executeQuery("SELECT DISTINCT o.tag FROM i2b2.OntNodeTag o WHERE o.tagtype='" + tagtype + "' order by o.tag")
        log.trace "${tags as JSON}"
        render(template: 'depSelectTerm', model: [tagtype: tagtype, tags: tags])
    }

    def ajaxOntTagFilter =
            {
                log.trace("called ajaxOntTagFilter")
                log.trace("tagterm:" + params.tagterm)
                def tagterm = params.tagterm
                def ontsearchterm = params.ontsearchterm
                def tagtype = params.tagtype
                def result = ontologyService.searchOntology(tagterm, [ontsearchterm], tagtype, 'JSON')
                render result as JSON
            }


    def getInitialSecurity =
            {
                def user = AuthUser.findByUsername(springSecurityService.getPrincipal().username)
                def result = i2b2HelperService.getAccess(i2b2HelperService.getRootPathsWithTokens(), user);
                render result as JSON
            }
    def sectest =
            {
                log.trace("KEYS:" + params.keys)
                def keys = params.keys.toString().split(",");
                def paths = [];
                def access;
                if (params.keys != "") {
                    keys.each { key ->
                        log.debug("in LOOP")
                        paths.add(i2b2HelperService.keyToPath(key))
                    }
                    def user = AuthUser.findByUsername(springSecurityService.getPrincipal().username)


                    access = i2b2HelperService.getConceptPathAccessCascadeForUser(paths, user)
                }
                log.trace(access as JSON)
            }

    def showConceptDefinition = {
        OntologyTerm term = conceptsResourceService.getByKey(params.conceptKey)
        Study study = term.study

        Experiment experiment
        FmFolder folder
        if (study?.ontologyTerm == term) {
            // is the top study term
            experiment = Experiment.findByAccession(study.id.toUpperCase())
            if (experiment) {
                folder = FmFolderAssociation.findByObjectUid(experiment.uniqueId?.uniqueId)?.fmFolder
            }
        }

        if (!folder) {
            log.debug "Could not find folder association; will look for tags"

            def tags = []
            // this solution is suboptimcal. The best would be to check that
            // the table is i2b2metadata.i2b2, but there is no API method
            // exposing that
            if (!term.key.startsWith('\\\\xtrials\\')) {
                tags = OntNodeTag.findAll(
                        'FROM OntNodeTag T WHERE T.ontnode.basecode = :code', [code: term.code])
            }
            render template: 'showDefinition', model: [tags: tags]
        } else {
            log.debug "Found experiment ($experiment) and folder association " +
                    "($folder); will not attempt to look for tags"

            AmTagTemplate amTagTemplate = amTagTemplateService.getTemplate(folder.uniqueId)
            List<AmTagItem> metaDataTagItems = amTagItemService.getDisplayItems(amTagTemplate?.id)
            render template: 'showStudy',
                    model: [folder          : folder,
                            bioDataObject   : experiment,
                            metaDataTagItems: metaDataTagItems]
        }
    }

    def showExtFiles = {
        OntologyTerm term = conceptsResourceService.getByKey(params.conceptKey)
        def conceptKey = escapeJavascript(null, params.conceptKey)
        def conceptid = escapeJavascript(null, params.conceptid)
        def conceptcomment = escapeJavascript(null, params.conceptcomment)
        def files = externalDataService.getExtDataForStudy(params.conceptKey)
        def manageAccess = externalDataService.hasManagePermission(params.conceptKey)
        render template: 'showExtFiles', model: [study: term.fullName, files: files, conceptKey: conceptKey, conceptid: conceptid, conceptcomment: conceptcomment, manageAccess: manageAccess]
    }

    def editExtFile = {
        def conceptKey = escapeJavascript(null, params.conceptKey)
        def conceptid = escapeJavascript(null, params.conceptid)
        def conceptcomment = escapeJavascript(null, params.conceptcomment)
        def fullName = new ConceptKey(params.conceptKey).conceptFullName.toString()
        def types = ExtDataType.findAll()
        def fileId = params.fileId
        def file = externalDataService.getExtData(Long.parseLong(fileId))
        render template: 'editExtFile', model: [study: fullName, types: types, file: file, conceptKey: conceptKey, conceptid: conceptid, conceptcomment: conceptcomment]
    }

    def addExtFile = {
        def conceptKey = escapeJavascript(null, params.conceptKey)
        def conceptid = escapeJavascript(null, params.conceptid)
        def conceptcomment = escapeJavascript(null, params.conceptcomment)
        def fullName = new ConceptKey(params.conceptKey).conceptFullName
        def types = ExtDataType.findAll()
        render template: 'addExtFile', model: [study: fullName, types: types, conceptKey: conceptKey, conceptid: conceptid, conceptcomment: conceptcomment]
    }

    def deleteExtFile = {
        externalDataService.deleteExtData(Long.parseLong(params.id))
        // we must return something to prevent error 404
        render "Done"
    }

    static String parseLogPas(String logPass) {
        String t = logPass.trim()
        return (t == "" ? null : t)
    }

    def editExtFileDone = {
        externalDataService.updateExtData(
                Long.parseLong(params.fileId),
                params.name,
                params.desc,
                ExtDataType.get(Long.parseLong(params.datatype_id)),
                params.link,
                parseLogPas(params.link_login),
                parseLogPas(params.link_password),
        )
        // we must return something to prevent error 404
        render "Done"
    }

    def addExtFileDone = {
        externalDataService.addExtData(
                params.conceptKey,
                params.name,
                params.desc,
                ExtDataType.get(Long.parseLong(params.datatype_id)),
                params.link,
                parseLogPas(params.link_login),
                parseLogPas(params.link_password),
        )
        // we must return something to prevent error 404
        render "Done"
    }
}
