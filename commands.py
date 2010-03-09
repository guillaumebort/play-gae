import tempfile

# GAE

if play_command.startswith('gae:'):
    gae_path = None
    try:
        optlist, args = getopt.getopt(remaining_args, '', ['gae='])
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
        print "~ This module has been tested with GAE 1.3.0"
        print "~ "
        sys.exit(-1)


if play_command == 'gae:deploy':
	
	print '~'
	print '~ Compiling'
	print '~ ---------'
	
	remaining_args = []
	check_application()
	load_modules()
	do_classpath()
	do_java()
	if os.path.exists(os.path.join(application_path, 'tmp')):
		shutil.rmtree(os.path.join(application_path, 'tmp'))
	if os.path.exists(os.path.join(application_path, 'precompiled')):
		shutil.rmtree(os.path.join(application_path, 'precompiled'))
	java_cmd.insert(2, '-Dprecompile=yes')
	try:
		subprocess.call(java_cmd, env=os.environ)
	except OSError:
		print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
		sys.exit(-1)
		
	if os.path.exists(os.path.join(application_path, 'tmp')):
		shutil.rmtree(os.path.join(application_path, 'tmp'))

	print '~'
	print '~ Packaging'
	print '~ ---------'

	war_path = os.path.join(tempfile.gettempdir(), '%s.war' % os.path.basename(application_path))
	package_as_war(war_path, None)

	print '~'
	print '~ Deploying'
	print '~ ---------'

	os.system('%s/bin/appcfg.sh update %s' % (gae_path, war_path))

	print "~ "
	print "~ Done!"
	print "~ "
	sys.exit(-1)