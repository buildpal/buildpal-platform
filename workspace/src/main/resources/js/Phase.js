var objectUtils   = require('./ObjectUtils.js');

var Phase = function() {
    this._id = null;
    this._name = null;
    this._description = null;

    this._env = {};
    this._repo = null;

    this._containerArgs = null;
    this._workingDir = null;
    this._preScript = null;
    this._mainScript = null;
};

Phase.prototype._initMain = function(shell, envs) {
    shell.run('#!/bin/sh');
    shell._newLine();

    for (var e=0; e<envs.length; e++) {
        for (var key in envs[e]) {
            shell.run('export ' + key + '="' + envs[e][key] + '"')
        }

        shell._newLine();
    }

    shell.run('cd $WORKSPACE_PATH');
    shell._newLine();
};

Phase.prototype.id = function(id) {
    if (id) {
        this._id = id;
        return this;
    }

    return this._id;
};

Phase.prototype.name = function(name) {
    if (name) {
        this._name = name;
        return this;
    }

    return this._name;
};

Phase.prototype.description = function(description) {
    if (description) {
        this._description = description;
        return this;
    }

    return this._description;
};

Phase.prototype.env = function(env) {
    if (env) {
        this._env = objectUtils.merge(this._env, env);
        return this;
    }

    return this._env;
};

Phase.prototype.repo = function(repo) {
    if (repo) {
        this._repo = repo;
        return this;
    }

    return this._repo;
};


Phase.prototype.conf = function(conf) {
    if (typeof conf !== 'function') {
        throw 'Configuration should be a function.';
    }

    this._pre = conf;

    return this;
};

Phase.prototype.exec = function(exec) {
    if (typeof exec !== 'function') {
        throw 'Executable should be a function.';
    }

    this._main = exec;

    return this;
};

module.exports = Phase;
