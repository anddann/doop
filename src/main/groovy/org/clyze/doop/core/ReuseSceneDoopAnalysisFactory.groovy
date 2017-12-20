package org.clyze.doop.core

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.clyze.analysis.AnalysisFamily
import org.clyze.analysis.AnalysisOption
import org.clyze.doop.input.DefaultInputResolutionContext
import org.clyze.doop.input.InputResolutionContext
import org.clyze.utils.CheckSum
import org.clyze.utils.FileOps

/**
 * A Factory for creating Analysis objects.
 *
 * All the methods invoked by newAnalysis (either directly or indirectly) could
 * have been static helpers (e.g. entailed in the Helper class) but they are
 * protected instance methods to allow descendants to customize all possible
 * aspects of Analysis creation.
 */
class ReuseSceneDoopAnalysisFactory extends DoopAnalysisFactory {

    Log logger = LogFactory.getLog(getClass())
    static final char[] EXTRA_ID_CHARACTERS = '_-+.'.toCharArray()
    static final String HASH_ALGO = "SHA-256"

    /**
     * Creates a new analysis, verifying the correctness of its id, name, options and inputFiles using
     * the supplied input resolution mechanism.
     * If the supplied id is empty or null, an id will be generated automatically.
     * Otherwise the id will be validated:
     * - if it is valid, it will be used to identify the analysis,
     * - if it is invalid, an exception will be thrown.
     */
    DoopAnalysis newAnalysis(String id, String name, Map<String, AnalysisOption> options, InputResolutionContext context) {

        def vars = processOptions(name, options, context)

        super.checkAnalysis(name)
        if (options.LB3.value)
            super.checkLogicBlox(vars)

        //init the environment used for executing commands
        Map<String, String> commandsEnv = super.initExternalCommandsEnvironment(vars)

        // if not empty or null
        def analysisId = this.generateId(vars)

        def cacheId = this.generateCacheID(vars)

        def outDir = super.createOutputDirectory(vars, analysisId)

        def cacheDir = new File("${Doop.doopCache}/$cacheId")
        checkAppGlob(vars)


        DoopAnalysis analysis
        if (options.LB3.value) {
            if (name != "sound-may-point-to") {
                options.CFG_ANALYSIS.value = false
                analysis = new ClassicAnalysis(
                        analysisId,
                        name.replace(File.separator, "-"),
                        options,
                        context,
                        outDir,
                        cacheDir,
                        vars.inputFiles,
                        vars.platformFiles,
                        commandsEnv)
            } else {
                analysis = new SoundMayAnalysis(
                        analysisId,
                        name.replace(File.separator, "-"),
                        options,
                        context,
                        outDir,
                        cacheDir,
                        vars.inputFiles,
                        vars.platformFiles,
                        commandsEnv)
            }
        } else {
            options.CFG_ANALYSIS.value = false
            analysis = new SouffleAnalysis(
                    analysisId,
                    name.replace(File.separator, "-"),
                    options,
                    context,
                    outDir,
                    cacheDir,
                    vars.inputFiles,
                    vars.platformFiles,
                    commandsEnv)
        }
        logger.debug "Created new analysis"
        return analysis
    }

    @Override
    protected String generateId(AnalysisVars vars) {
        Collection<String> idComponents = vars.options.keySet().findAll {
            !Doop.OPTIONS_EXCLUDED_FROM_ID_GENERATION.contains(it)
        }.collect {
            String option -> return vars.options.get(option).toString()
        }
        idComponents = [vars.name] + idComponents
        logger.debug("ID components: $idComponents")
        def id = idComponents.join('-')

        return CheckSum.checksum(id, HASH_ALGO)
    }

    @Override
    protected String generateCacheID(AnalysisVars vars) {
        Collection<String> idComponents = vars.options.values()
                .findAll { it.forCacheID }
                .collect { option -> option.toString() }

        Collection<String> checksums = []
        //FIXME: optimize checksum computation
        //checksums += CheckSum.checksum(new File(userDir), HASH_ALGO)
        int value = new Random().nextInt();
        checksums += value.toString()
        if (vars.options.TAMIFLEX.value && vars.options.TAMIFLEX.value != "dummy")
            checksums += [CheckSum.checksum(new File(vars.options.TAMIFLEX.value.toString()), HASH_ALGO)]

        idComponents = checksums + idComponents

        logger.debug("Cache ID components: $idComponents")
        def id = idComponents.join('-')

        return CheckSum.checksum(id, HASH_ALGO)
    }

    /**
     * Creates a new analysis, verifying the correctness of its name, options and inputFiles using
     * the default input resolution mechanism.
     */
    DoopAnalysis newAnalysis(AnalysisFamily family, String id, String name, Map<String, AnalysisOption> options) {
        DefaultInputResolutionContext context = new DefaultInputResolutionContext()
        return newAnalysis(id, name, options, context)
    }

    /**
     * Processes the options of the analysis.
     */
    protected AnalysisVars processOptions(String name, Map<String, AnalysisOption> options, InputResolutionContext context) {

        logger.debug "Processing analysis options"


        if (options.MAIN_CLASS.value) {
            logger.debug "The main class is set to ${options.MAIN_CLASS.value}"
        } else {
            logger.debug "\nWARNING: No main class was found. This will trigger open-program analysis!\n"
        }

        if (options.DYNAMIC.value) {
            List<String> dynFiles = options.DYNAMIC.value as List<String>
            dynFiles.each { String dynFile ->
                FileOps.findFileOrThrow(dynFile, "The DYNAMIC option is invalid: ${dynFile}")
                logger.debug "The DYNAMIC option has been set to ${dynFile}"
            }
        }

        if (options.TAMIFLEX.value && options.TAMIFLEX.value != "dummy") {
            def tamFile = options.TAMIFLEX.value.toString()
            FileOps.findFileOrThrow(tamFile, "The TAMIFLEX option is invalid: ${tamFile}")
        }


        if (options.DISTINGUISH_ALL_STRING_BUFFERS.value &&
                options.DISTINGUISH_STRING_BUFFERS_PER_PACKAGE.value) {
            logger.warn "\nWARNING: multiple distinguish-string-buffer flags. 'All' overrides.\n"
        }

        if (options.NO_MERGE_LIBRARY_OBJECTS.value) {
            options.MERGE_LIBRARY_OBJECTS_PER_METHOD.value = false
        }

        if (options.MERGE_LIBRARY_OBJECTS_PER_METHOD.value && options.CONTEXT_SENSITIVE_LIBRARY_ANALYSIS.value) {
            logger.warn "\nWARNING, possible inconsistency: context-sensitive library analysis with merged objects.\n"
        }

        if (options.DISTINGUISH_REFLECTION_ONLY_STRING_CONSTANTS.value) {
            options.DISTINGUISH_REFLECTION_ONLY_STRING_CONSTANTS.value = true
            options.DISTINGUISH_ALL_STRING_CONSTANTS.value = false
        }

        if (options.DISTINGUISH_ALL_STRING_CONSTANTS.value) {
            options.DISTINGUISH_REFLECTION_ONLY_STRING_CONSTANTS.value = false
            options.DISTINGUISH_ALL_STRING_CONSTANTS.value = true
        }

        if (options.REFLECTION_CLASSIC.value) {
            options.DISTINGUISH_ALL_STRING_CONSTANTS.value = false
            options.DISTINGUISH_REFLECTION_ONLY_STRING_CONSTANTS.value = true
            options.REFLECTION.value = true
            options.REFLECTION_SUBSTRING_ANALYSIS.value = true
            options.DISTINGUISH_STRING_BUFFERS_PER_PACKAGE.value = true
        }

        if (options.TAMIFLEX.value) {
            options.REFLECTION.value = false
        }

        if (options.MINIMAL_INFORMATION_FLOW.value) {
            options.INFORMATION_FLOW.value = options.MINIMAL_INFORMATION_FLOW.value
        }

        if (options.NO_SSA.value) {
            options.SSA.value = false
        }

        if (options.MUST.value) {
            options.MUST_AFTER_MAY.value = true
        }

        if (options.X_DRY_RUN.value) {
            options.X_STATS_NONE.value = true
            options.X_SERVER_LOGIC.value = true
            if (options.CACHE.value) {
                logger.warn "\nWARNING: Doing a dry run of the analysis while using cached facts might be problematic!\n"
            }
        }

        if (!options.REFLECTION.value) {
            if (options.DISTINGUISH_REFLECTION_ONLY_STRING_CONSTANTS.value ||
                    options.REFLECTION_SUBSTRING_ANALYSIS.value ||
                    options.REFLECTION_CONTEXT_SENSITIVITY.value ||
                    options.REFLECTION_HIGH_SOUNDNESS_MODE.value ||
                    options.REFLECTION_SPECULATIVE_USE_BASED_ANALYSIS.value ||
                    options.REFLECTION_INVENT_UNKNOWN_OBJECTS.value ||
                    options.REFLECTION_REFINED_OBJECTS.value) {
                logger.warn "\nWARNING: Probable inconsistent set of Java reflection flags!\n"
            } else if (options.TAMIFLEX.value) {
                logger.warn "\nWARNING: Handling of Java reflection via Tamiflex logic!\n"
            } else {
                logger.warn "\nWARNING: Handling of Java reflection is disabled!\n"
            }
        }

        options.values().each {
            if (it.argName && it.value && it.validValues && !(it.value in it.validValues))
                throw new RuntimeException("Invalid value `$it.value` for option: $it.name")
        }

        options.values().findAll { it.isMandatory }.each {
            if (!it.value) throw new RuntimeException("Missing mandatory argument: $it.name")
        }

        logger.debug "---------------"
        AnalysisVars vars = new AnalysisVars(
                name: name,
                options: options,
                inputFilePaths: null,
                platformFilePaths: null,
                inputFiles: null,
                platformFiles: null
        )
        logger.debug vars
        logger.debug "---------------"

        return vars
    }

    /**
     * Determines application classes.
     *
     * If an app regex is not present, it generates one.
     */
    protected void checkAppGlob(AnalysisVars vars) {
        if (!vars.options.APP_REGEX.value) {
            logger.debug "Generating app regex"

            //We process only the first jar for determining the application classes
            /*
            Set excluded = ["*", "**"] as Set
            analysis.jars.drop(1).each { Dependency jar ->
                excluded += Helper.getPackages(jar.input())
            }

            Set<String> packages = Helper.getPackages(analysis.jars[0].input()) - excluded
            */
            Set<String> packages = new HashSet<>()
            for(soot.SootClass kl:soot.Scene.v().getApplicationClasses()){
                packages.add(kl.getPackageName())
            }
            vars.options.APP_REGEX.value = packages.sort().join(':')
        }
    }

}
