/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

/* global ContactsX, ContactXError */

exports.defineAutoTests = function () {
	describe('ContactsX (window.ContactsX)', function () {
		it('ContactsX.spec.1 should exist', function () {
			expect(window.ContactsX).toBeDefined();
		});

		it('ContactsX.spec.2 should have the correct properties ', function () {
			expect(window.ContactsX.ErrorCodes).toBeDefined();
			expect(window.ContactsX.ErrorCodes.CanceledAction).toBeDefined();
			expect(window.ContactsX.ErrorCodes.NotFound).toBeDefined();
			expect(window.ContactsX.ErrorCodes.PermissionDenied).toBeDefined();
			expect(window.ContactsX.ErrorCodes.UnknownError).toBeDefined();
			expect(window.ContactsX.ErrorCodes.UnsupportedAction).toBeDefined();
			expect(window.ContactsX.ErrorCodes.WrongJsonObject).toBeDefined();

			expect(window.ContactsX.ErrorCodes.CanceledAction).toBe(4);
			expect(window.ContactsX.ErrorCodes.NotFound).toBe(5);
			expect(window.ContactsX.ErrorCodes.PermissionDenied).toBe(3);
			expect(window.ContactsX.ErrorCodes.UnknownError).toBe(10);
			expect(window.ContactsX.ErrorCodes.UnsupportedAction).toBe(1);
			expect(window.ContactsX.ErrorCodes.WrongJsonObject).toBe(2);
		});

		it('ContactsX.spec.3 should contain a delete function', function () {
			expect(window.ContactsX.delete).toBeDefined();
			expect(typeof window.ContactsX.delete === 'function').toBe(true);
		});

		it('ContactsX.spec.4 should contain a find function', function () {
			expect(window.ContactsX.find).toBeDefined();
			expect(typeof window.ContactsX.find === 'function').toBe(true);
		});

		it('ContactsX.spec.5 should contain a hasPermission function', function () {
			expect(window.ContactsX.hasPermission).toBeDefined();
			expect(typeof window.ContactsX.hasPermission === 'function').toBe(true);
		});

		it('ContactsX.spec.6 should contain a pick function', function () {
			expect(window.ContactsX.pick).toBeDefined();
			expect(typeof window.ContactsX.pick === 'function').toBe(true);
		});

		it('ContactsX.spec.7 should contain a requestPermission function', function () {
			expect(window.ContactsX.requestPermission).toBeDefined();
			expect(typeof window.ContactsX.requestPermission === 'function').toBe(true);
		});

		it('ContactsX.spec.8 should contain a requestWritePermission function', function () {
			expect(window.ContactsX.requestWritePermission).toBeDefined();
			expect(typeof window.ContactsX.requestWritePermission === 'function').toBe(true);
		});

		it('ContactsX.spec.9 should contain a save function', function () {
			expect(window.ContactsX.save).toBeDefined();
			expect(typeof window.ContactsX.save === 'function').toBe(true);
		});
	});
};

/******************************************************************************/
/******************************************************************************/
/******************************************************************************/

exports.defineManualTests = function (contentEl, createActionButton) {
	var pageStartTime = +new Date();

	function log(value, value2) {
		if (typeof value == "object")
			value = JSON.stringify(value, null, 2);

		if (typeof value2 == "object")
			value2 = JSON.stringify(value2, null, 2);

		var result = (new Date() - pageStartTime) / 1000 + ': ' + value + '\n';

		if (typeof value2 != 'undefined')
			result += '\n' + value2 + '\n';

		document.getElementById('contactx_status').textContent += result;

		console.log(value + "\n" + value2);
	}

	function logError(value, value2) {
		if (typeof value == "object")
			value = JSON.stringify(value);

		if (typeof value2 == "object")
			value2 = JSON.stringify(value2);

		var result = (new Date() - pageStartTime) / 1000 + ': ' + value + '\n';

		if (typeof value2 != 'undefined')
			result += '\n' + value2 + '\n';

		document.getElementById('contactx_status').textContent += result;

		console.error(value + "\n" + value2);
	}

	function hasPermission() {
		clearStatus();
		window.ContactsX.hasPermission(function (success) {
			log('permissions: ', success);
		}, function (error) {
			logError('error checking permissions', error);
		});
	}

	function requestContactPermission() {
		clearStatus();
		window.ContactsX.requestPermission(function (success) {
			log('requested contact permission success');
		}, function (error) {
			logError('denied contact permission ', error);
		});
	}

	function requestWritePermission() {
		clearStatus();
		window.ContactsX.requestWritePermission(function (success) {
			log('requested write permission success');
		}, function (error) {
			logError('denied write permission ', error);
		});
	}

	function save() {
		const sample = {
			firstName: "Hans",
			middleName: "Hubert",
			familyName: "Test",
			organizationName: "Einfach",
			phoneNumbers: [{
				type: "home",
				value: "12341234"
			},
			{
				type: "mobile",
				value: "+49 151 12345"
			},
			{
				type: "work",
				value: "+1 (424) 555-1234"
			},
			{
				type: "mobile",
				value: "(06) 123 4567"
			},
			{
				type: "other",
				value: "+31 (0) 6 987 654"
			}
			],
			emails: [
				{
					value: "Hans.Test@somemail.io",
					type: "home"
				}, {
					value: "HansHubert@someworkmail.com",
					type: "work"
				}
			],
			addresses: [
				{
					street: "Ahmet Arif Blv. 141",
					city: "Merkez",
					region: "Batman",
					postCode: "72070",
					country: "Turkey",
					type: "home"
				},
				{
					street: "10 Cockhill Rd",
					city: "Ballymacarry",
					region: "Donegal",
					postCode: "F93",
					country: "Ireland",
					type: "work"
				},
				{
					street: "9 Front Rd",
					city: "Dildo",
					region: "Newfoundland",
					postCode: "A0B 1P0",
					country: "Canada",
					type: "other"
				},
				{
					street: "17000 Kickapoo Rd",
					city: "Leavenworth",
					region: "66048",
					postCode: "Kansas",
					country: "USA",
					type: "rama lama ding dong"
				}
			],
			baseCountryCode: "DE"
		}

		clearStatus();
		window.ContactsX.save(sample,
			function (success) {
				log('wrote sample ', success);
			},
			function (error) {
				logError(error);
			});
	}


	function findAllFields(countryCode) {
		clearStatus();
		window.ContactsX.find(function (success) {
			log('find contacts ', success);
		}, function (error) {
			logError(error);
		}, {
			fields: {
				displayName: true,
				firstName: true,
				middleName: true,
				familyName: true,
				organizationName: true,
				phoneNumbers: true,
				email: true,
				addresses: true
			},
			baseCountryCode: countryCode
		});
	}

	function findOneField() {
		clearStatus();
		window.ContactsX.find(function (success) {
			log('find contacts ', success);
		}, function (error) {
			logError(error);
		}, {
			fields: {
				firstName: true,
				displayName: false,
				middleName: false,
				familyName: false,
				organizationName: false,
				phoneNumbers: false,
				email: false
			},
			baseCountryCode: ""
		});
	}

	function pick() {
		clearStatus();
		window.ContactsX.pick(function (success) {
			log('picked contact ', success);
		}, function (error) {
			logError(error);
		});
	}

	function deleteId(id) {
		clearStatus();
		window.ContactsX.delete(id,
			function (success) {
				console.log("removed contact", success);
			},
			function (error) {
				logError(error);
			});
	}

	function clearStatus() {
		document.getElementById('contactx_status').innerHTML = '';
	}

	/******************************************************************************/

	contentEl.innerHTML =
		'<div id="info" style="white-space: pre-wrap">' +
		'<b>Status:</b> <div id="contactx_status"></div>' +

		'</div><div id="hasPermission"></div>' +
		'Expected result: If no Permission was granted, should throw error' +

		'</div><div id="requestContactPermission"></div>' +
		'Expected result: Shows permission request and should return success if accepted' +

		'</div><div id="requestWritePermission"></div>' +
		'Expected result: Shows permission request to write to contacts' +

		'</div><div id="save"></div>' +
		'Expected result: Saves a predefined contact' +

		'</div><input type="text" id="countryCode" value="DE"/></div>' +
		'</div><div id="findAllFields"></div>' +
		'Expected result: Reads all contacts and lists all fields' +

		'</div><div id="findOneField"></div>' +
		'Expected result: Reads all contacts and lists all firstnames' +

		'</div><div id="pick"></div>' +
		'Expected result: Opens the system contact picker and lists fields' +

		'</div><div id="deleteId"></div>' +
		'</div><input type="text" id="deleteInput"/></div>' +
		'Expected result: Deletes predefined contact by ID'

	createActionButton(
		'Check Permission',
		function () {
			hasPermission();
		},
		'hasPermission'
	);

	createActionButton(
		'Request Contact Permission',
		function () {
			requestContactPermission();
		},
		'requestContactPermission'
	);

	createActionButton(
		'Request Write Permission',
		function () {
			requestWritePermission();
		},
		'requestWritePermission'
	);

	createActionButton(
		'Save a Contact',
		function () {
			save();
		},
		'save'
	);

	createActionButton(
		'List all Contacts with all Fields',
		function () {
			findAllFields(document.getElementById("countryCode").value);
		},
		'findAllFields'
	);

	createActionButton(
		'List all Contacts firstName only',
		function () {
			findOneField();
		},
		'findOneField'
	);

	createActionButton(
		'Pick a Contact',
		function () {
			pick();
		},
		'pick'
	);

	createActionButton(
		'Delete saved Contact',
		function () {
			deleteId(document.getElementById("deleteInput").value);
		},
		'deleteId'
	);
};