var socket = null;
var receiveText = "";
var port = null;
var screen = {callback:"cbScreen"};
initalizeSocket();

var sendResponse = function(data) {
	if (!port) return;
	if (!data) return;

	port.postMessage(data);
};

function copy(str) {
    var sandbox = $('#sandbox').val(str).select();
    document.execCommand('copy');
    sandbox.val('');
}

function paste() {
    var result = '',
        sandbox = $('#sandbox').val('').select();
    if (document.execCommand('paste')) {
        result = sandbox.val();
    }
    sandbox.val('');
    return result;
}

function getScope(id) {
	var queryId = typeof id == typeof undefined ? "body" : id;
	return angular.element(queryId).scope();
}

function changeVar(variable,val) {
	if (variable in getScope()) {
		var oldVar = getScope()[variable];
		if (oldVar == val) return;
		getScope()[variable] = val;
		//getScope().$apply();
	}
}

function pasteLoop() {
	var data = paste();
	changeVar("copiedText", data);
}

var otherCopying = false;

var app = angular.module("myApp", []);
var interval;
app.controller("myCtrl", function($scope, $interval){
	$scope.copiedText = paste();
	$scope.$watch("copiedText", function(n,o) {
		if (receiveText == n) return;
		
		if (otherCopying) {
			otherCopying = false;
			return;
		}
		
		if (n == o) return;
		if (socket.connected) {
			var copiedText = n;
			if (copiedText.length > 0) {
				var dataCopied = {"from":"chrome","copiedText":copiedText};
				socket.emit("dataCopied", dataCopied);
				var response = {callback: "cbDataCopied", data: dataCopied};
				sendResponse(response);
			}
		}
	});
	
	interval = $interval(pasteLoop, 1000);
});


function initalizeSocket () {
	socket = io("http://calisma.herokuapp.com/");
	//socket = io("http://localhost:3000/");

	socket.on("otherCopied", function(data){
		if (typeof data != "object") return;
		if (!("copiedText" in data)) return;
		var copiedText = data.copiedText;
		otherCopying = true;
		receiveText = copiedText;
		copy(copiedText);
		changeVar("copiedText", copiedText);
	});

	socket.on("connect", function() {

	});

	socket.on("connectRoom", function(data) {
		data.callback = "cbConnectRoom";
		screen.roomStatus = 2;
		sendResponse(data);
	});

	socket.on("roomUpdated", function(data) {
		data.callback = "cbRoomUpdated";
		screen.room = data;
		sendResponse(data);
	});

	socket.on("disconnect", function(){
		screen.roomStatus = 0;
	});

};

chrome.runtime.onConnect.addListener(function(p) {
	port = p;
	p.onMessage.addListener(function(request){
		if (!("type" in request)) return;

		if (eval("typeof "+request.type+" == 'function'")) {
			var params = request.data ? JSON.stringify(request.data):"";
			eval(request.type+"("+params+");");
		}
	});

	p.postMessage(screen);
	
});

function connectRoom (data) {
	socket.emit("connectRoom", {"from":data.from,"roomName":data.roomName,"hasRoom":data.hasRoom,"name":data.name});
}

function disconnectRoom () {
	socket.emit("disconnectRoom");
	screen.roomStatus = 0;
}
 
