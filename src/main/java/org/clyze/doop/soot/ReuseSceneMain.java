package org.clyze.doop.soot;

import com.google.common.base.Optional;
import heros.solver.CountingThreadPoolExecutor;
import org.clyze.doop.common.Database;
import org.clyze.doop.soot.android.AndroidManifest;
import org.clyze.doop.util.filter.GlobClassFilter;
import org.objectweb.asm.ClassReader;
import soot.*;
import soot.SourceLocator.FoundFile;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.resources.LayoutControl;
import soot.options.Options;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import static org.clyze.doop.soot.android.AndroidManifest.getAndroidManifest;
import static soot.DexClassProvider.classesOfDex;
import static soot.jimple.infoflow.android.InfoflowAndroidConfiguration.CallbackAnalyzer.Fast;

public class ReuseSceneMain {

    private static int shift(String[] args, int index) {
        if (args.length == index + 1) {
            System.err.println("error: option " + args[index] + " requires an argument");
            System.exit(1);
        }
        return index + 1;
    }

    private static boolean isApplicationClass(SootParameters sootParameters, SootClass klass) {
        sootParameters.applicationClassFilter = new GlobClassFilter(sootParameters.appRegex);

        return sootParameters.applicationClassFilter.matches(klass.getName());
    }

    public static void main(String[] args) {
        SootParameters sootParameters = new SootParameters();
        try {
            if (args.length == 0) {
                System.err.println("usage: [options] file...");
                throw new DoopErrorCodeException(0);
            }

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--full":
                        if (sootParameters._mode != null) {
                            System.err.println("error: duplicate mode argument");
                            throw new DoopErrorCodeException(1);
                        }
                        sootParameters._mode = SootParameters.Mode.FULL;
                        break;
                    case "-d":
                        i = shift(args, i);
                        sootParameters._outputDir = args[i];
                        break;
                    case "--main":
                        i = shift(args, i);
                        sootParameters._main = args[i];
                        break;
                    case "--ssa":
                        sootParameters._ssa = true;
                        break;
                    case "--android-jars":
                        i = shift(args, i);
                        sootParameters._allowPhantom = true;
                        sootParameters._android = true;
                        sootParameters._androidJars = args[i];
                        break;
                    case "-l":
                        i = shift(args, i);
                        sootParameters._libraries.add(args[i]);
                        break;
                    case "-lsystem":
                        String javaHome = System.getProperty("java.home");
                        sootParameters._libraries.add(javaHome + File.separator + "lib" + File.separator + "rt.jar");
                        sootParameters._libraries.add(javaHome + File.separator + "lib" + File.separator + "jce.jar");
                        sootParameters._libraries.add(javaHome + File.separator + "lib" + File.separator + "jsse.jar");
                        break;
                    case "--deps":
                        i = shift(args, i);
                        String folderName = args[i];
                        File f = new File(folderName);
                        if (!f.exists()) {
                            System.err.println("Dependency folder " + folderName + " does not exist");
                            throw new DoopErrorCodeException(0);
                        } else if (!f.isDirectory()) {
                            System.err.println("Dependency folder " + folderName + " is not a directory");
                            throw new DoopErrorCodeException(0);
                        }
                        for (File file : f.listFiles()) {
                            if (file.isFile() && file.getName().endsWith(".jar")) {
                                sootParameters._libraries.add(file.getCanonicalPath());
                            }
                        }
                        break;
                    case "--application-regex":
                        i = shift(args, i);
                        sootParameters.appRegex = args[i];
                        break;
                    case "--allow-phantom":
                        sootParameters._allowPhantom = true;
                        break;
                    case "--run-flowdroid":
                        sootParameters._runFlowdroid = true;
                        break;
                    case "--only-application-classes-fact-gen":
                        sootParameters._onlyApplicationClassesFactGen = true;
                        break;
                    case "--generate-jimple":
                        sootParameters._generateJimple = true;
                        break;
                    case "--stdout":
                        sootParameters._toStdout = true;
                        break;
                    case "--noFacts":
                        sootParameters._noFacts = true;
                        break;
                    case "--uniqueFacts":
                        sootParameters._uniqueFacts = true;
                        break;
                    //FIXME: added module mode parameter
                    case "--modulemode":
                        sootParameters._moduleMode = true;
                        break;
                    case "--modulename":
                        i = shift(args, i);
                        sootParameters._moduleName = args[i];
                        break;
                    case "-h":
                    case "--help":
                    case "-help":
                        System.err.println("\nusage: [options] file");
                        System.err.println("options:");
                        System.err.println("  --main <class>                        Specify the main name of the main class");
                        System.err.println("  --ssa                                 Generate SSA facts, enabling flow-sensitive analysis");
                        System.err.println("  --full                                Generate facts by full transitive resolution");
                        System.err.println("  -d <directory>                        Specify where to generate csv fact files");
                        System.err.println("  -l <archive>                          Find classes in jar/zip archive");
                        System.err.println("  -lsystem                              Find classes in default system classes");
                        System.err.println("  --deps <directory>                    Add jars in this directory to the class lookup path");
                        System.err.println("  --only-application-classes-fact-gen   Generate facts only for application classes");
                        System.err.println("  --noFacts                             Don't generate facts (just empty files -- used for debugging)");
                        System.err.println("  --uniqueFacts                         Eliminate redundancy from facts");

                        System.err.println("  --generate-jimple                     Generate Jimple/Shimple files instead of facts");
                        System.err.println("  --generate-jimple-help                Show help information regarding bytecode2jimple");
                        System.err.println("  --modulemode                          Analyze Java 9 modules");
                        System.err.println("  --modulename                          Name of the java 9 module to analyze");

                        throw new DoopErrorCodeException(0);
                    case "--generate-jimple-help":
                        System.err.println("\nusage: [options] file");
                        System.err.println("options:");
                        System.err.println("  --ssa                                 Generate Shimple files (use SSA for variables)");
                        System.err.println("  --full                                Generate Jimple/Shimple files by full transitive resolution");
                        System.err.println("  --stdout                              Write Jimple/Shimple to stdout");
                        System.err.println("  -d <directory>                        Specify where to generate files");
                        System.err.println("  -l <archive>                          Find classes in jar/zip archive");
                        System.err.println("  -lsystem                              Find classes in default system classes");
                        System.err.println("  --android-jars <archive>              The main android library jar (for android apks). The same jar should be provided in the -l option");
                        throw new DoopErrorCodeException(0);
                    default:
                        if (args[i].charAt(0) == '-') {
                            System.err.println("error: unrecognized option: " + args[i]);
                            throw new DoopErrorCodeException(0);
                        } else {
                            sootParameters._inputs.add(args[i]);
                        }
                        break;
                }
            }

            if (sootParameters._mode == null) {
                sootParameters._mode = SootParameters.Mode.INPUTS;
            }

            if (sootParameters._toStdout && !sootParameters._generateJimple) {
                System.err.println("error: --stdout must be used with --generate-jimple");
                throw new DoopErrorCodeException(1);
            }
            if (sootParameters._toStdout && sootParameters._outputDir != null) {
                System.err.println("error: --stdout and -d options are not compatible");
                throw new DoopErrorCodeException(2);
            } else if ((sootParameters._inputs.stream().filter(s -> s.endsWith(".apk")).count() > 0) &&
                    (!sootParameters._android)) {
                System.err.println("error: the --platform parameter is mandatory for .apk inputs");
                throw new DoopErrorCodeException(3);
            } else if (!sootParameters._toStdout && sootParameters._outputDir == null) {
                sootParameters._outputDir = System.getProperty("user.dir");
            }
            produceFacts(sootParameters);
        } catch (DoopErrorCodeException errCode) {
            int n = errCode.getErrorCode();
            if (n != 0)
                System.err.println("Exiting with code " + n);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    public static void produceFacts(SootParameters sootParameters) throws Exception {
        SootMethod dummyMain = null;

        Options.v().set_output_dir(sootParameters._outputDir);

        if (sootParameters._ssa) {
            Options.v().set_via_shimple(true);
            Options.v().set_output_format(Options.output_format_shimple);
        } else {
            Options.v().set_output_format(Options.output_format_jimple);
        }
        //soot.options.Options.v().set_drop_bodies_after_load(true);
        Options.v().set_keep_line_number(true);

        PropertyProvider propertyProvider = new PropertyProvider();
        Set<SootClass> classes = new HashSet<>();
        Set<String> classesInApplicationJar = new HashSet<>();
        Set<String> appServices = null;
        Set<String> appActivities = null;
        Set<String> appContentProviders = null;
        Set<String> appBroadcastReceivers = null;
        Set<String> appCallbackMethods = null;
        Set<LayoutControl> appUserControls = null;
        File apk = null;

        if (sootParameters._android) {
            String apkLocation = sootParameters._inputs.get(0);
            apk = new File(apkLocation);
            SetupApplication app = new SetupApplication(sootParameters._androidJars, apkLocation);
            Options.v().set_process_multiple_dex(true);
            Options.v().set_src_prec(Options.src_prec_apk);

            if (sootParameters._runFlowdroid) {
                app.getConfig().setCallbackAnalyzer(Fast);
                String filename = ReuseSceneMain.class.getClassLoader().getResource("SourcesAndSinks.txt").getFile();
                //FIXME: adann no clue
               // app.calculateSourcesSinksEntrypoints(filename);
                dummyMain = app.getDummyMainMethod();
                if (dummyMain == null) {
                    throw new RuntimeException("Dummy main null");
                }
            } else {
                AndroidManifest processMan = getAndroidManifest(apkLocation);
                String appPackageName = processMan.getPackageName();

                // now collect the facts we need
                appServices = processMan.getServices();
                appActivities = processMan.getActivities();
                appContentProviders = processMan.getProviders();
                appBroadcastReceivers = processMan.getReceivers();
                appCallbackMethods = processMan.getCallbackMethods();
                appUserControls = processMan.getUserControls();

//            System.out.println("All entry points:\n" + appEntrypoints);
//            System.out.println("\nServices:\n" + appServices + "\nActivities:\n" + appActivities + "\nProviders:\n" + appContentProviders + "\nCallback receivers:\n" +appBroadcastReceivers);
//            System.out.println("\nCallback methods:\n" + appCallbackMethods + "\nUser controls:\n" + appUserControls);
                processMan.printManifestInfo();
            }

        }

        Scene scene = null;
        if (sootParameters._moduleMode) {
            scene = ModuleScene.v();
          } else {
            scene = Scene.v();
        }

        //Scene already exists

       /* for (String lib : sootParameters._libraries) {
            System.out.println("Adding archive for resolving: " + lib);
            scene.extendSootClassPath(lib);
        }*/

        if (sootParameters._main != null) {
            Options.v().set_main_class(sootParameters._main);
        }

        if (sootParameters._mode == SootParameters.Mode.FULL) {
            Options.v().set_full_resolver(true);
        }

        if (sootParameters._allowPhantom) {
            Options.v().set_allow_phantom_refs(true);
        }


        classes.stream().filter((klass) -> isApplicationClass(sootParameters, klass)).forEachOrdered(SootClass::setApplicationClass);

        if (sootParameters._mode == SootParameters.Mode.FULL && !sootParameters._onlyApplicationClassesFactGen) {
            classes = new HashSet<>(scene.getClasses());
        }

        System.out.println("Total classes in Scene: " + classes.size());
        try {
            retrieveAllSceneClassesBodies();
            System.out.println("Retrieved all bodies");
        } catch (Exception ex) {
            System.out.println("Not all bodies retrieved");
        }
        Database db = new Database(new File(sootParameters._outputDir), sootParameters._uniqueFacts);
        FactWriter writer = new FactWriter(db);
        ThreadFactory factory = new ThreadFactory(writer, sootParameters._ssa, sootParameters._generateJimple);
        Driver driver = new Driver(factory, classes.size(), sootParameters._generateJimple);

        classes.stream().filter(SootClass::isApplicationClass).forEachOrdered(writer::writeApplicationClass);

        // Read all stored properties files
        for (Map.Entry<String, Properties> entry : propertyProvider.getProperties().entrySet()) {
            String path = entry.getKey();
            Properties properties = entry.getValue();

            for (String propertyName : properties.stringPropertyNames()) {
                String propertyValue = properties.getProperty(propertyName);
                writer.writeProperty(path, propertyName, propertyValue);
            }
        }

        db.flush();

        if (sootParameters._android) {
            if (sootParameters._runFlowdroid) {
                driver.doAndroidInSequentialOrder(dummyMain, classes, writer, sootParameters._ssa);
                db.close();
                return;
            } else {
                String apkLocation = sootParameters._inputs.get(0);
                AndroidManifest processMan = getAndroidManifest(apkLocation);

                if (processMan.getApplicationName() != null)
                    writer.writeApplication(processMan.expandClassName(processMan.getApplicationName()));
                else {
                    // If no application name, use Android's Application:
                    // "The fully qualified name of an Application subclass
                    // implemented for the application. ... In the absence of a
                    // subclass, Android uses an instance of the base
                    // Application class."
                    // https://developer.android.com/guide/topics/manifest/application-element.html
                    writer.writeApplication("android.app.Application");
                }
                for (String s : appActivities) {
                    writer.writeActivity(processMan.expandClassName(s));
                }

                for (String s : appServices) {
                    writer.writeService(processMan.expandClassName(s));
                }

                for (String s : appContentProviders) {
                    writer.writeContentProvider(processMan.expandClassName(s));
                }

                for (String s : appBroadcastReceivers) {
                    writer.writeBroadcastReceiver(processMan.expandClassName(s));
                }

                for (String callbackMethod : appCallbackMethods) {
                    writer.writeCallbackMethod(callbackMethod);
                }

              /*  for (LayoutControl possibleLayoutControl : appUserControls) {
                    writer.writeLayoutControl(possibleLayoutControl.getID(), possibleLayoutControl.getViewClassName(), possibleLayoutControl.getParentID());
                    if (possibleLayoutControl.isSensitive()) {
                        writer.writeSensitiveLayoutControl(possibleLayoutControl.getID(), possibleLayoutControl.getViewClassName(), possibleLayoutControl.getParentID());
                    }
                }*/
            }
        }
        if (!sootParameters._noFacts) {

            scene.getOrMakeFastHierarchy();
            // avoids a concurrent modification exception, since we may
            // later be asking soot to add phantom classes to the scene's hierarchy
            driver.doInParallel(classes);
        }

        db.close();
    }



    public static void retrieveAllSceneClassesBodies() {
        // The old coffi front-end is not thread-safe
        int threadNum = Options.v().coffi() ? 1 : Runtime.getRuntime().availableProcessors();
        CountingThreadPoolExecutor executor = new CountingThreadPoolExecutor(threadNum, threadNum, 30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        Iterator<SootClass> clIt = Scene.v().getClasses().snapshotIterator();
        while (clIt.hasNext()) {
            SootClass cl = (SootClass) clIt.next();
            // note: the following is a snapshot iterator;
            // this is necessary because it can happen that phantom methods
            // are added during resolution
            //FIXME: added case for module info (not required module-info files)
            if(cl.resolvingLevel()<SootClass.SIGNATURES && (cl instanceof SootModuleInfo))
                continue;

            Iterator<SootMethod> methodIt = cl.getMethods().iterator();
            while (methodIt.hasNext()) {
                final SootMethod m = methodIt.next();
                if (m.isConcrete()) {
                    executor.execute(new Runnable() {

                        @Override
                        public void run() {
                            m.retrieveActiveBody();
                        }

                    });
                }
            }
        }

        // Wait till all method bodies have been loaded
        try {
            executor.awaitCompletion();
            executor.shutdown();
        } catch (InterruptedException e) {
            // Something went horribly wrong
            throw new RuntimeException("Could not wait for loader threads to " + "finish: " + e.getMessage(), e);
        }

        // If something went wrong, we tell the world
        if (executor.getException() != null)
            throw (RuntimeException) executor.getException();
    }
}
