// (c) 2014 Don Coleman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/* global mainPage, deviceList, refreshButton */
/* global detailPage, resultDiv, pulseWidthInput, motorButton, buzzerButton, disconnectButton */
/* global ble, cordova */
/* jshint browser: true , devel: true*/
'use strict';

// ASCII only
function bytesToString(buffer) {
    return String.fromCharCode.apply(null, new Uint8Array(buffer));
}

// ASCII only
function stringToBytes(string) {
    var array = new Uint8Array(string.length);
    for (var i = 0, l = string.length; i < l; i++) {
        array[i] = string.charCodeAt(i);
    }
    return array.buffer;
}

// this is Nordic's UART service
var bluefruit = {
    serviceUUID: "0000fff0-0000-1000-8000-00805f9b34fb",
    txCharacteristic: "0000fff2-0000-1000-8000-00805f9b34fb", // transmit is from the phone's perspective
    rxCharacteristic: "0000fff1-0000-1000-8000-00805f9b34fb"  // receive is from the phone's perspective
};

var app = {
    initialize: function() {
        this.bindEvents();
        detailPage.hidden = true;
    },
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
        refreshButton.addEventListener('touchstart', this.refreshDeviceList, false);
        sendButton.addEventListener('click', this.sendData, false);
        disconnectButton.addEventListener('touchstart', this.disconnect, false);
        deviceList.addEventListener('touchstart', this.connect, false); // assume not scrolling
    },
    onDeviceReady: function() {
        app.refreshDeviceList();
    },
    refreshDeviceList: function() {
        deviceList.innerHTML = ''; // empties the list
        if (cordova.platformId === 'android') { // Android filtering is broken
            ble.scan([], 5, app.onDiscoverDevice, app.onError);
        } else {
            ble.scan([bluefruit.serviceUUID], 5, app.onDiscoverDevice, app.onError);
        }
    },
    onDiscoverDevice: function(device) {
        var listItem = document.createElement('li'),
            html = '<b>' + device.name + '</b><br/>' +
                'RSSI: ' + device.rssi + '&nbsp;|&nbsp;' +
                device.id;

        listItem.dataset.deviceId = device.id;
        listItem.innerHTML = html;
        deviceList.appendChild(listItem);
    },
    connect: function(e) {
        var deviceId = e.target.dataset.deviceId;

        var onConnect = function(peripheral) {
            sendButton.dataset.deviceId = deviceId;
            disconnectButton.dataset.deviceId = deviceId;

            //var socket = io.connect('http://10.29.2.168:15100/');
            //socket.on('keyboard message to other client event', function (data) {
            //    console.log(data.action.action);
            //    var commandData = '0';
            //    if(data.action.action === 'w') {
            //        commandData = "X1";
            //    }
            //    if(data.action.action === 'a') {
            //        commandData = "X2";
            //    }
            //    if(data.action.action === 's') {
            //        commandData = "X3";
            //    }
            //    if(data.action.action === 'd') {
            //        commandData = "X4";
            //    }
            //
            //    ble.writeWithoutResponse(
            //        deviceId,
            //        bluefruit.serviceUUID,
            //        bluefruit.txCharacteristic,
            //        stringToBytes(commandData), null, null
            //    );
            //});

            //manager.on("move", function(result, data){
            //    var commandData = "";
            //    if (data.direction.angle === "left") {
            //        commandData = "X2";
            //    }
            //    if (data.direction.angle === "right") {
            //        commandData = "X3";
            //    }
            //    if (data.direction.angle === "up") {
            //        commandData = "X1";
            //    }
            //    if (data.direction.angle === "down") {
            //        commandData = "X4";
            //    }
            //
            //    ble.writeWithoutResponse(
            //        deviceId,
            //        bluefruit.serviceUUID,
            //        bluefruit.txCharacteristic,
            //        stringToBytes(commandData), null, null
            //    );
            //});
            app.showDetailPage();

        };

        ble.connect(deviceId, onConnect, app.onError);
    },
    onData: function(data) { // data received from Arduino
        resultDiv.innerHTML = resultDiv.innerHTML + "Received: " + bytesToString(data) + "<br/>";
        resultDiv.scrollTop = resultDiv.scrollHeight;
    },
    sendData: function(event) { // send data to Arduino
        var success = function(result) {
            resultDiv.innerHTML = resultDiv.innerHTML + "Sent: " + messageInput.value + "<br/>";
            resultDiv.scrollTop = resultDiv.scrollHeight;
        };

        var failure = function(error) {
            alert("Failed writing data to the bluefruit le: " + JSON.stringify(error));
        };

        var data = stringToBytes(messageInput.value);
        var deviceId = event.target.dataset.deviceId;

        ble.writeWithoutResponse(
            deviceId,
            bluefruit.serviceUUID,
            bluefruit.txCharacteristic,
            data, success, failure
        );
    },
    disconnect: function(event) {
        var deviceId = event.target.dataset.deviceId;
        ble.disconnect(deviceId, app.showMainPage, app.onError);
    },
    showMainPage: function() {
        mainPage.hidden = false;
        detailPage.hidden = true;
    },
    showDetailPage: function() {
        mainPage.hidden = true;
        detailPage.hidden = false;
    },
    onError: function(reason) {
        alert("ERROR: " + JSON.stringify(reason)); // real apps should use notification.alert
    }
};
