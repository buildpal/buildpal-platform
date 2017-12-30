
exports.vertxStart = function() {
    vertx.eventBus().localConsumer(__buildID__, function(mh) {
        var cmd = mh.body();

        if (cmd === 'dryRun') {
            pipeline.dryRunSuccess = dryRun();
            mh.reply(pipeline);

        } else {
            console.error('Unknown message sent: ' + cmd);
        }
    });
}

function dryRun() {
    try {
        for (var s=0; s<pipeline._stages.length; s++) {
            var phases = pipeline._stages[s];

            for (var p=0; p<phases.length; p++) {
                var phase = phases[p];
                dryRunPhase(phase);
            }
        }

        return true;

    } catch (ex) {
        console.error('Dry-run for build: ' + __buildID__ + ' threw an error. Ex: ' + ex);
        return false;
    }
}

function dryRunPhase(phase) {
    // Change workspace path if a child repo is specified.
    // TODO: Validate multi repo and child.
    if (phase.repo()) {
        var moreEnv = {};
        moreEnv[Workspace.PATH] = globalEnv[Workspace.PATH] + '/' + phase.repo();
        phase.env(moreEnv);
    }

    var workspace = new Workspace([globalEnv, pipeline.env(), phase.env()]);
    var containerArgs = new ContainerArgs();
    var container = new Container();

    // Write output to the shell script.
    phase._pre.call(null, containerArgs, workspace);
    phase._containerArgs = containerArgs;
    phase._preScript = workspace._toScript()

    phase._initMain.call(null, container.shell(), [globalEnv, pipeline.env(), phase.env()]);
    phase._main.call(null, container);
    phase._docker = container.docker;
    phase._mainScript = container.shell()._toScript();
}
