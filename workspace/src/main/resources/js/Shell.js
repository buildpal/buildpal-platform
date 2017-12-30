var StringBuilder = Java.type('java.lang.StringBuilder');

var NEW_LINE = '\n';

var Shell = function() {
    this._buffer = new StringBuilder();
};

Shell.prototype._newLine = function() {
    this._buffer.append(NEW_LINE);
};

Shell.prototype.run = function(command) {
    this._buffer.append(command).append(NEW_LINE);
}

Shell.prototype._toScript = function() {
    return this._buffer.toString()
}

module.exports = Shell;