var Shell = require("./Shell.js");

var Workspace = function(envs) {
    this._shell = new Shell();

    this._shell.run('#!/bin/sh');
    this._shell._newLine();

    for (var e=0; e<envs.length; e++) {
        for (var key in envs[e]) {
            this._shell.run('export ' + key + '="' + envs[e][key] + '"')
        }

        this._shell._newLine();
    }

    this._updated = false;
};

Workspace.prototype.grantRWXToAll = function() {
    // Give full access to the workspace.
    this._shell.run('chmod -R o=rwx $' + Workspace.PATH);

    this._updated = true;
};

Workspace.prototype._toScript = function() {
    return this._updated ? this._shell._toScript() : null;
}

Workspace.PATH = 'WORKSPACE_PATH';

module.exports = Workspace;