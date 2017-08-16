var socket = io("http://calisma.herokuapp.com/");
//var socket = io("http://localhost:3000");

var receiveText = "";

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
				socket.emit("dataCopied", {"from":"chrome","a":"b","copiedText":copiedText});
			}
		}
	});
	
	interval = $interval(pasteLoop, 1000);
});


socket.on("otherCopied", function(data){
	//console.log(data);//bbbaasad
	if (typeof data != "object") return;
	if (!("copiedText" in data)) return;
	var copiedText = data.copiedText;
	otherCopying = true;
	receiveText = copiedText;
	copy(copiedText);
	changeVar("copiedText", copiedText);
});
