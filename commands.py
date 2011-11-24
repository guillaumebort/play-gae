import tempfile
import getopt
import os, os.path
import sys
import shutil
import subprocess

try:
    from play.utils import package_as_war
    PLAY10 = False
except ImportError:
    PLAY10 = True

# GAE

MODULE = "gae"

COMMANDS = ["gae:deploy", "gae:update_indexes", "gae:vacuum_indexes", "gae:update_queues", "gae:update_dos", "gae:update_cron", "gae:cron_info", "gae:request_logs"]
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


		package_as_war(app, env, war_path, None)

    
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
