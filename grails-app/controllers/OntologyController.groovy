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
import groovy.json.StringEscapeUtils

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
                    model: [folder: folder,
                            bioDataObject: experiment,
                            metaDataTagItems: metaDataTagItems]
        }
    }

    def showExtFiles = {
        OntologyTerm term = conceptsResourceService.getByKey(params.conceptKey)
        def conceptKey = escapeJavascript(null,params.conceptKey)
        def conceptid = escapeJavascript(null,params.conceptid)
        def conceptcomment = escapeJavascript(null,params.conceptcomment)
        def files = ExtData.findAll(' FROM ExtData ED WHERE ED.study = :study', [study: term.fullName])
            render template: 'showExtFiles', model: [study : term.fullName, files : files, conceptKey : conceptKey, conceptid : conceptid, conceptcomment : conceptcomment ]
    }

    def editExtFile = {
        def conceptKey = escapeJavascript(null,params.conceptKey)
        def conceptid = escapeJavascript(null,params.conceptid)
        def conceptcomment = escapeJavascript(null,params.conceptcomment)
        OntologyTerm term = conceptsResourceService.getByKey(params.conceptKey)
        def types = ExtDataType.findAll(' FROM ExtDataType')
        def fileId = params.fileId
        def file = ExtData.get(params.fileId)
        render template: 'editExtFile', model: [study : term.fullName, types : types, file: file, conceptKey : conceptKey, conceptid : conceptid, conceptcomment : conceptcomment]
    }

    def addExtFile = {
        def conceptKey = escapeJavascript(null,params.conceptKey)
        def conceptid = escapeJavascript(null,params.conceptid)
        def conceptcomment = escapeJavascript(null,params.conceptcomment)
        OntologyTerm term = conceptsResourceService.getByKey(params.conceptKey)
        def types = ExtDataType.findAll(' FROM ExtDataType')
        render template: 'addExtFile', model: [study : term.fullName, types : types, conceptKey : conceptKey, conceptid : conceptid, conceptcomment : conceptcomment]
    }

    def deleteExtFile = {
        def extDataFile = ExtData.get(params.id)
        def fullname = extDataFile.study+extDataFile.name+' (ext)\\'
        def jobID = null
        groovy.sql.Sql sql = new groovy.sql.Sql(dataSource)
        sql.call("{call TM_CZ.I2B2_DELETE_ALL_NODES($fullname,$jobID)}")
        sql.close()
        extDataFile.delete()

    }

    def editExtFileDone = {
        OntologyTerm term = conceptsResourceService.getByKey(params.conceptKey)
        def path = term.fullName+params.name+' (ext)'+'\\'
        def oldname = params.oldName;
        def oldPath = term.fullName+oldname+' (ext)'
        def trialID = term.study.id
        def pathName =params.name+' (ext)'
        def jobid=null
        groovy.sql.Sql sql = new groovy.sql.Sql(dataSource)
        sql.call("{call TM_CZ.I2B2_DELETE_ALL_NODES($oldPath,$jobid)}")
        sql.call("{call TM_CZ.I2B2_ADD_NODE($trialID,$path,$pathName, $jobid)}")
        def datatype = Integer.parseInt(params.datatype_id)
        if(datatype==1) {
            sql.executeUpdate("update i2b2metadata.i2b2 set C_VISUALATTRIBUTES='LA' where C_FULLNAME = $path")
        } else {
            sql.executeUpdate("update i2b2metadata.i2b2 set C_VISUALATTRIBUTES='LAH' where C_FULLNAME = $path")
        }
        sql.close()
        def newData = ExtData.get(params.fileId)
        newData.name=params.name
        newData.description=params.desc
        newData.link=params.link
        newData.study=term.fullName
        newData.dataType=ExtDataType.get(Integer.parseInt(params.datatype_id))
        newData.save()
    }

    def addExtFileDone = {
        print("ALARMDONE!"+params.conceptKey)
        OntologyTerm term = conceptsResourceService.getByKey(params.conceptKey)
        def path = term.fullName+params.name+' (ext)'+'\\'
        def trialID = term.study.id
        def pathName =params.name+' (ext)'
        def jobid=null
        groovy.sql.Sql sql = new groovy.sql.Sql(dataSource)
        sql.call("{call TM_CZ.I2B2_ADD_NODE($trialID,$path,$pathName, $jobid)}")
        def datatype = Integer.parseInt(params.datatype_id)
        if(datatype==1) {
            sql.executeUpdate("update i2b2metadata.i2b2 set C_VISUALATTRIBUTES='LA' where C_FULLNAME = $path")
        } else {
            sql.executeUpdate("update i2b2metadata.i2b2 set C_VISUALATTRIBUTES='LAH' where C_FULLNAME = $path")
        }
        sql.close()
        def newData = new ExtData()
        newData.name=params.name
        newData.description=params.desc
        newData.link=params.link
        newData.study=term.fullName
        newData.dataType=ExtDataType.get(Integer.parseInt(params.datatype_id))
        newData.save()

        print("SaveDONE!"+params.conceptKey)
    }
}
