var objectUtils   = require('./ObjectUtils.js');

var Link = function(image) {
    this._image = image;

    this._as = null;
    this._portMappings = [];
};

Link.prototype.image = function(image) {
    if (image) {
        this._image = image;
        return this;
    }

    return this._image;
};

Link.prototype.as = function(as) {
    if (as) {
        this._as = as;
        return this;
    }

    return _as;
};

Link.prototype.mapPort = function(portMapping) {
    if (portMapping) {
        this._portMappings.push(portMapping);
    }

    return this;
};

var Links = function() {
    this.links = [];
};

Links.prototype.add = function(image) {
    var link = new Link(image);

    this.links.push(link);

    return link;
};

var ContainerArgs = function() {
    this._rawArgs = {};

    this.links = new Links();
};

ContainerArgs.prototype.image = function(image) {
    if (image) {
        this._rawArgs['Img'] = image;
        return this;
    }

    return this._rawArgs['Img'];
};

ContainerArgs.prototype.user = function(user) {
    if (user) {
        this._rawArgs['User'] = user;
        return this;
    }

    return this._rawArgs['User'];
};

ContainerArgs.prototype.rawArgs = function(args) {
    if (args) {
        this._rawArgs = objectUtils.merge(this._rawArgs, args);
        return this;
    }

    return this._rawArgs;
};


module.exports = ContainerArgs;
