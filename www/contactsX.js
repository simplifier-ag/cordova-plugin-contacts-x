var exec = require('cordova/exec');

var contactsX = {
    ErrorCodes: {
        UnsupportedAction: 1,
        WrongJsonObject: 2,
        PermissionDenied: 3,
        CanceledAction: 4,
        NotFound: 5,
        UnknownError: 10
    },

    find: function (success, error, options) {
        window.ContactsX._checkReadPermission(function () {
            exec(success, error, 'ContactsX', 'find', [options]);
        }, error);
    },

    pick: function (success, error) {
        window.ContactsX._checkReadPermission(function () {
            exec(success, error, 'ContactsX', 'pick', []);
        }, error);
    },

    save: function (contact, success, error) {
        window.ContactsX._checkWritePermission(function () {
            exec(success, error, 'ContactsX', 'save', [contact]);
        }, error);
    },

    delete: function (id, success, error) {
        window.ContactsX._checkWritePermission(function () {
            exec(success, error, 'ContactsX', 'delete', [id]);
        }, error);
    },

    hasPermission: function (success, error) {
        exec(success, error, 'ContactsX', 'hasPermission', []);
    },

    requestPermission: function (success, error) {
        exec(success, error, 'ContactsX', 'requestPermission', [false]);
    },

    requestWritePermission: function (success, error) {
        exec(success, error, 'ContactsX', 'requestPermission', [true]);
    },

    _checkReadPermission: function (success, error) {
        window.ContactsX.hasPermission(function (permission) {
            if (permission.read) {
                success();
                return;
            }

            window.ContactsX.requestPermission(function (permission) {
                if (permission.read) {
                    success();
                    return;
                }

                error(window.ContactsX.ErrorCodes.PermissionDenied);
            }, error);
        }, error);
    },

    _checkWritePermission: function (success, error) {
        window.ContactsX.hasPermission(function (permission) {
            if (permission.write) {
                success();
                return;
            }

            window.ContactsX.requestWritePermission(function (permission) {
                if (permission.write) {
                    success();
                    return;
                }

                error(window.ContactsX.ErrorCodes.PermissionDenied);
            }, error);
        }, error);
    }
}

module.exports = contactsX;
