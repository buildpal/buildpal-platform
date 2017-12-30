var ObjectUtils = function() {

    this.merge = function() {
        var mergedObj = {};

        for (var a=0; a<arguments.length; a++) {
            var objFrom = arguments[a];

            if (typeof objFrom !== 'object') {
                'Please pass a valid object (at index: ' + a + ')';
            }

            for (var prop in objFrom) {
                mergedObj[prop] = objFrom[prop];
            }
        }

        return mergedObj;
    };
};

module.exports = new ObjectUtils();
