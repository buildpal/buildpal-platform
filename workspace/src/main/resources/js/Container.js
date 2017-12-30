var Shell = require('./Shell.js');

var Docker = function(listener) {
    this._tags = [];
    this._foldersToCopy = [];

    this._listener = listener;

    this._buildEnabled = false;
    this._pushEnabled = false;
    this._copyWorkspace = false;
};

Docker.prototype.tags = function() {
    if (arguments && arguments.length > 0) {
        for (var a=0; a<arguments.length; a++) {
            var tag = arguments[a];

            if (typeof tag !== 'string') throw 'Please add a valid tag (at index: ' + a + ')';

            this._tags.push(tag);
        }

        return this;
    }

    return this._tags;
};

Docker.prototype.copyWorkspace = function() {
    this._copyWorkspace = true;

    return this;
};

Docker.prototype.copyFolder = function(folder) {
    if (!folder) throw 'Folder to copy should be valid relative path';

    this._foldersToCopy.push(folder);

    return this;
};

Docker.prototype.build = function() {
    this._buildEnabled = true;
    this._notify();

    return this;
};

Docker.prototype.push = function() {
    this._pushEnabled = true;
    this._notify();

    return this;
};

Docker.prototype._notify = function() {
    if (this._listener) {
        this._listener.call();
    }
};

var NoShell = function() {
    this.run = function() {
        throw 'Cannot run shell command after calling docker.build() or docker.push()';
    }
};

var Container = function() {
    this._shell = new Shell();

    this.docker = new Docker(this._dockerCalled);
};

Container.prototype.shell = function() {
    return this._shell;
};

Container.prototype.sh = function(command) {
    this._shell.run(command);

    return this;
};

// docker build listener.
Container.prototype._dockerCalled = function() {
    this._shell = new NoShell();
};

module.exports = Container;
