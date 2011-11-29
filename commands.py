import tempfile
import getopt
import os, os.path
import sys
import shutil
import subprocess

try:
    from play.utils import isParentOf, copy_directory, replaceAll
    PLAY10 = False
except ImportError:
    PLAY10 = True

# GAE

MODULE = "gae"

COMMANDS = ["gae:deploy", "gae:package", "gae:update_indexes", "gae:vacuum_indexes", "gae:update_queues", "gae:update_dos", "gae:update_cron", "gae:cron_info", "gae:request_logs"]
HELP = {
    'gae:deploy': "Deploy to Google App Engine",
    'gae:update_indexes': "Updating Indexes",
    'gae:vacuum_indexes': "Deleting Unused Indexes",
    'gae:update_queues': "Managing Task Queues",
    'gae:update_dos': "Managing DoS Protection",
    'gae:update_cron': "Managing Scheduled Tasks : upload cron job specifications",
    'gae:cron_info': "Managing Scheduled Tasks : verify your cron configuration",
    'gae:request_logs': "Download logs from Google App Engine",
}


def find(f, seq):
  """Return first item in sequence where f(item) == True."""
  for item in seq:
    if f(item): 
      return item

def package_as_gae_war(app, env, war_path, war_zip_path, war_exclusion_list = None):
    if war_exclusion_list is None:
        war_exclusion_list = []
    app.check()
    modules = app.modules()
    classpath = app.getClasspath()

    if not war_path:
        print "~ Oops. Please specify a path where to generate the WAR, using the -o or --output option"
        print "~"
        sys.exit(-1)

    if os.path.exists(war_path) and not os.path.exists(os.path.join(war_path, 'WEB-INF')):
        print "~ Oops. The destination path already exists but does not seem to host a valid WAR structure"
        print "~"
        sys.exit(-1)

    if isParentOf(app.path, war_path):
        print "~ Oops. Please specify a destination directory outside of the application"
        print "~"
        sys.exit(-1)

    print "~ Packaging current version of the framework and the application to %s ..." % (os.path.normpath(war_path))
    if os.path.exists(war_path): shutil.rmtree(war_path)
    if os.path.exists(os.path.join(app.path, 'war')):
        copy_directory(os.path.join(app.path, 'war'), war_path)
    else:
        os.makedirs(war_path)
    if not os.path.exists(os.path.join(war_path, 'WEB-INF')): os.mkdir(os.path.join(war_path, 'WEB-INF'))
    if not os.path.exists(os.path.join(war_path, 'WEB-INF/web.xml')):
        shutil.copyfile(os.path.join(env["basedir"], 'resources/war/web.xml'), os.path.join(war_path, 'WEB-INF/web.xml'))
    application_name = app.readConf('application.name')
    replaceAll(os.path.join(war_path, 'WEB-INF/web.xml'), r'%APPLICATION_NAME%', application_name)
    if env["id"] is not "":
        replaceAll(os.path.join(war_path, 'WEB-INF/web.xml'), r'%PLAY_ID%', env["id"])
    else:
        replaceAll(os.path.join(war_path, 'WEB-INF/web.xml'), r'%PLAY_ID%', 'war')
    if os.path.exists(os.path.join(war_path, 'WEB-INF/application')): shutil.rmtree(os.path.join(war_path, 'WEB-INF/application'))
    copy_directory(app.path, os.path.join(war_path, 'WEB-INF/application'), war_exclusion_list)
    if os.path.exists(os.path.join(war_path, 'WEB-INF/application/war')):
        shutil.rmtree(os.path.join(war_path, 'WEB-INF/application/war'))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/application/logs')):
        shutil.rmtree(os.path.join(war_path, 'WEB-INF/application/logs'))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/application/tmp')):
        shutil.rmtree(os.path.join(war_path, 'WEB-INF/application/tmp'))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/application/modules')):
        shutil.rmtree(os.path.join(war_path, 'WEB-INF/application/modules'))
    copy_directory(os.path.join(app.path, 'conf'), os.path.join(war_path, 'WEB-INF/classes'))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/lib')): shutil.rmtree(os.path.join(war_path, 'WEB-INF/lib'))
    os.mkdir(os.path.join(war_path, 'WEB-INF/lib'))
    for jar in classpath:
	# SPECIFIC GAE : excludes from the libs all provided and postgres/mysql/jdbc libs
	# keeps appengine-api only
	# appengine-api-labs removed
	gae_excluded = [ 
			'provided-', 'postgres', 'mysql', 'jdbc', 
			'appengine-agent',  'appengine-agentimpl',
			'appengine-agentruntime', 'appengine-api-stubs', 
			'appengine-local-runtime', 'appengine-testing',
			'appengine-api-labs'
	]
        if jar.endswith('.jar'):
		if find(lambda excl: excl in jar, gae_excluded): 
			print "~ Excluding JAR %s ..." % jar
		else:
	            shutil.copyfile(jar, os.path.join(war_path, 'WEB-INF/lib/%s' % os.path.split(jar)[1]))
    if os.path.exists(os.path.join(war_path, 'WEB-INF/framework')): shutil.rmtree(os.path.join(war_path, 'WEB-INF/framework'))
    os.mkdir(os.path.join(war_path, 'WEB-INF/framework'))
    copy_directory(os.path.join(env["basedir"], 'framework/templates'), os.path.join(war_path, 'WEB-INF/framework/templates'))

    # modules
    for module in modules:
        to = os.path.join(war_path, 'WEB-INF/application/modules/%s' % os.path.basename(module))
        copy_directory(module, to)
        if os.path.exists(os.path.join(to, 'src')):
            shutil.rmtree(os.path.join(to, 'src'))
        if os.path.exists(os.path.join(to, 'dist')):
            shutil.rmtree(os.path.join(to, 'dist'))
        if os.path.exists(os.path.join(to, 'samples-and-tests')):
            shutil.rmtree(os.path.join(to, 'samples-and-tests'))
        if os.path.exists(os.path.join(to, 'build.xml')):
            os.remove(os.path.join(to, 'build.xml'))
        if os.path.exists(os.path.join(to, 'commands.py')):
            os.remove(os.path.join(to, 'commands.py'))
        if os.path.exists(os.path.join(to, 'lib')):
            shutil.rmtree(os.path.join(to, 'lib'))
        if os.path.exists(os.path.join(to, 'nbproject')):
            shutil.rmtree(os.path.join(to, 'nbproject'))
        if os.path.exists(os.path.join(to, 'documentation')):
            shutil.rmtree(os.path.join(to, 'documentation'))

    if not os.path.exists(os.path.join(war_path, 'WEB-INF/resources')): os.mkdir(os.path.join(war_path, 'WEB-INF/resources'))
    shutil.copyfile(os.path.join(env["basedir"], 'resources/messages'), os.path.join(war_path, 'WEB-INF/resources/messages'))

    if war_zip_path:
        print "~ Creating zipped archive to %s ..." % (os.path.normpath(war_zip_path))
        if os.path.exists(war_zip_path):
            os.remove(war_zip_path)
        zip = zipfile.ZipFile(war_zip_path, 'w', zipfile.ZIP_STORED)
        dist_dir = os.path.join(app.path, 'dist')
        for (dirpath, dirnames, filenames) in os.walk(war_path):
            if dirpath == dist_dir:
                continue
            if dirpath.find('/.') > -1:
                continue
            for file in filenames:
                if file.find('~') > -1 or file.startswith('.'):
                    continue
                zip.write(os.path.join(dirpath, file), os.path.join(dirpath[len(war_path):], file))

        zip.close()

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    gae_path = None
    war_path = os.path.join(tempfile.gettempdir(), '%s.war' % os.path.basename(app.path))

    try:
        optlist, args2 = getopt.getopt(args, '', ['gae='])
        for o, a in optlist:
            if o == '--gae':
                gae_path = os.path.normpath(os.path.abspath(a))
                
    except getopt.GetoptError, err:
        print "~ %s" % str(err)
        print "~ "
        sys.exit(-1)

    if not gae_path and os.environ.has_key('GAE_PATH'):
        gae_path = os.path.normpath(os.path.abspath(os.environ['GAE_PATH']))

    if not gae_path:
        print "~ You need to specify the path of you GAE installation, "
        print "~ either using the $GAE_PATH environment variable or with the --gae option" 
        print "~ "
        sys.exit(-1)

    # check
    if not os.path.exists(os.path.join(gae_path, 'bin/appcfg.sh')):
        print "~ %s seems not to be a valid GAE installation (checked for bin/appcfg.sh)" % gae_path
        print "~ This module has been tested with GAE 1.5.0"
        print "~ "
        sys.exit(-1)
        
    for a in args:
         if a.find('--gae') == 0:
             args.remove(a)

    if command == "gae:deploy":
		print '~'
		print '~ Compiling'
		print '~ ---------'

		remaining_args = []
		app.check()
		java_cmd = app.java_cmd(args)
		if os.path.exists(os.path.join(app.path, 'tmp')):
		    shutil.rmtree(os.path.join(app.path, 'tmp'))
		if os.path.exists(os.path.join(app.path, 'precompiled')):
		    shutil.rmtree(os.path.join(app.path, 'precompiled'))
		java_cmd.insert(2, '-Dprecompile=yes')
		try:
		    result = subprocess.call(java_cmd, env=os.environ)
		    if not result == 0:
		        print "~"
		        print "~ Precompilation has failed, stop deploying."
		        print "~"
		        sys.exit(-1)
		    
		except OSError:
		    print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
		    sys.exit(-1)

		if os.path.exists(os.path.join(app.path, 'tmp')):
		    shutil.rmtree(os.path.join(app.path, 'tmp'))

		print '~'
		print '~ Packaging'
		print '~ ---------'


		package_as_gae_war(app, env, war_path, None)

    
		print '~'
		print '~ Deploying'
		print '~ ---------'

		if os.name == 'nt':
			os.system('%s/bin/appcfg.cmd update %s' % (gae_path, war_path))
		else:
			os.system('%s/bin/appcfg.sh update %s' % (gae_path, war_path))

		print "~ "
		print "~ Done!"
		print "~ "
		sys.exit(-1)
    if command == "gae:package":
		print '~'
		print '~ Compiling'
		print '~ ---------'

		remaining_args = []
		app.check()
		java_cmd = app.java_cmd(args)
		if os.path.exists(os.path.join(app.path, 'tmp')):
		    shutil.rmtree(os.path.join(app.path, 'tmp'))
		if os.path.exists(os.path.join(app.path, 'precompiled')):
		    shutil.rmtree(os.path.join(app.path, 'precompiled'))
		java_cmd.insert(2, '-Dprecompile=yes')
		try:
		    result = subprocess.call(java_cmd, env=os.environ)
		    if not result == 0:
		        print "~"
		        print "~ Precompilation has failed, stop deploying."
		        print "~"
		        sys.exit(-1)
		    
		except OSError:
		    print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
		    sys.exit(-1)

		if os.path.exists(os.path.join(app.path, 'tmp')):
		    shutil.rmtree(os.path.join(app.path, 'tmp'))

		print '~'
		print '~ Packaging'
		print '~ ---------'

		package_as_gae_war(app, env, war_path, None)
		print "~ "
		print "~ Done!"
		print "~ "
		sys.exit(-1)
    if command == "gae:update_indexes":
		print '~'
		print '~ Updating indexes'
		print '~ ---------'

		if os.name == 'nt':
				os.system('%s/bin/appcfg.cmd update_indexes %s' % (gae_path, war_path))
		else:
				os.system('%s/bin/appcfg.sh update_indexes %s' % (gae_path, war_path))

		print "~ "
		print "~ Done!"
		print "~ "
		sys.exit(-1)
    if command == "gae:vacuum_indexes":
		print '~'
		print '~ Deleting Unused Indexes'
		print '~ ---------'

		if os.name == 'nt':
				os.system('%s/bin/appcfg.cmd vacuum_indexes %s' % (gae_path, war_path))
		else:
				os.system('%s/bin/appcfg.sh vacuum_indexes %s' % (gae_path, war_path))

		print "~ "
		print "~ Done!"
		print "~ "
		sys.exit(-1)
    if command == "gae:update_queues":
		print '~'
		print '~ Updating Task Queues'
		print '~ ---------'

		if os.name == 'nt':
				os.system('%s/bin/appcfg.cmd update_queues %s' % (gae_path, war_path))
		else:
				os.system('%s/bin/appcfg.sh update_queues %s' % (gae_path, war_path))

		print "~ "
		print "~ Done!"
		print "~ "
		sys.exit(-1)
    if command == "gae:update_dos":
		print '~'
		print '~ Updating DoS Protection'
		print '~ ---------'

		if os.name == 'nt':
				os.system('%s/bin/appcfg.cmd update_dos %s' % (gae_path, war_path))
		else:
				os.system('%s/bin/appcfg.sh update_dos %s' % (gae_path, war_path))

		print "~ "
		print "~ Done!"
		print "~ "
		sys.exit(-1)
    if command == "gae:update_cron":
		print '~'
		print '~ Updating cron job specifications'
		print '~ ---------'

		if os.name == 'nt':
				os.system('%s/bin/appcfg.cmd update_cron %s' % (gae_path, war_path))
		else:
				os.system('%s/bin/appcfg.sh update_cron %s' % (gae_path, war_path))

		print "~ "
		print "~ Done!"
		print "~ "
		sys.exit(-1)
    if command == "gae:request_logs":
		print '~'
		print '~ Downloading Logs'
		print '~ ---------'

		if os.name == 'nt':
			os.system('%s/bin/appcfg.cmd request_logs %s ./logs/production.log' % (gae_path, war_path))
		else:
			os.system('%s/bin/appcfg.sh request_logs %s ./logs/production.log' % (gae_path, war_path))

		print "~ "
		print "~ Done!"
		print "~ "
		sys.exit(-1)
