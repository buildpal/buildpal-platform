var objectUtils = require('./ObjectUtils.js');
var Phase       = require('./Phase.js');

var Pipeline = function(id, name) {
    this._id = id;
    this._name = name;
    this._description = null;

    this._env = {};
    this._stages = [];
};

Pipeline.prototype.id = function(id) {
    if (id) {
        this._id = id;
        return this;
    }

    return this._id;
};

Pipeline.prototype.name = function(name) {
    if (name) {
        this._name = name;
        return this;
    }

    return this._name;
};

Pipeline.prototype.description = function(description) {
    if (description) {
        this._description = description;
        return this;
    }

    return this._description;
};

Pipeline.prototype.env = function(env) {
    if (env) {
        this._env = objectUtils.merge(this._env, env);
        return this;
    }

    return this._env;
};

Pipeline.prototype.add = function() {
    var parallelPhases = [];
    this._stages.push(parallelPhases);

    for (var a=0; a<arguments.length; a++) {
        var phase = arguments[a];

        if (!(phase instanceof Phase)) throw 'Please add a valid phase (at index: ' + a + ')';

        parallelPhases.push(phase);
        phase.id('' + this._stages.length + '_' + parallelPhases.length);
    }

    return this;
};

module.exports = Pipeline;
