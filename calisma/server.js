const express = require("express");
var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);

var port = process.env.PORT || 3000;

/*app.use(express.static("public"));

app.get('/', function(req, res){
  res.sendFile('/index.html');
});*/

//

const digitChars = ["A","B","C","D","E","F","G","H","J","K","L","M","N","P","R","S","T","U","W","X","V","Z","1","2","3","4","5","6","7","8","9"];
const connectRoomKeys = Object.keys({"from":null,"roomName":null,"hasRoom":null});
var rooms = {};

function dataError(data,socket) {	
	if (typeof data != "object") {
		socket.disconnect();
		return true;
	}
	return false;
}

function jsonError(data, filterArray, socket) {	
	if (typeof data != "object") {
		socket.disconnect();
		return true;
	}
	
	try {
		filterArray.forEach(function(val, index) {
			if (!data.hasOwnProperty(val)) {
				return true;
			}
		});
	
	} catch (e) {
		console.log(e);
		return true;
	}
	
	return false;
}

function createRoom () {
	var testNumber = 0;
	var digitLength = 4;
	var digit = "0000";
	do {
		if (testNumber == Math.pow(digitChars.length, digitLength) - 10/*garanted*/) digitLength++;
		digit = giveDigit(digitLength);
		testNumber++;
	} while (rooms.hasOwnProperty(digit));
	return digit;
}

function giveDigit (digitLength) {
	var digit = "";
	for (i = 0; i < digitLength;i++) {
		digit += digitChars[Math.floor(Math.random() * digitChars.length)];
	}
	return digit;
}

function logoutRoom (socket) {
	if (socket.infos.roomName) {
		if(rooms.hasOwnProperty(socket.infos.roomName)) {
			var room = rooms[socket.infos.roomName];
			if (Object.keys(room.users).length <= 1) {
				delete rooms[socket.infos.roomName];
			} else {
				delete rooms[socket.infos.roomName].users[socket.infos.userId];
			}
		}
	}
	
	socket.infos.roomName = null;
	socket.infos.room = null;
	
}


io.on('connection', function(socket){
  console.log('a user connected');
  
  var userId = giveDigit(2) + Number(new Date()).toString();
  socket.infos = {roomName:null,room:null,userId:userId};
    
  socket.on("dataCopied", function(data){
	  if (dataError(data,socket)) return;
	  
	  //bura hatli gibi
	  var userOnRoom = Object.keys(rooms[socket.infos.roomName].users);
	  userOnRoom.forEach(function(val,index) {
		  if (val == socket.infos.userId)
			  return;
		  var roomSocket = rooms[socket.infos.roomName].users[val].socket;
		  roomSocket.emit("otherCopied", data);
	  });
  });
  
  socket.on("connectRoom", function(data){
	  if (dataError(data,socket)) return;
	  if (jsonError(data, connectRoomKeys, socket)) return;
	  var roomName = data.roomName;
	  
	  if (socket.infos.roomName) {
		  logoutRoom(socket);
	  }
	  
	  if (!data.hasRoom) {
		  if (!rooms.hasOwnProperty(roomName)) {
			  roomName = createRoom();
			  rooms[roomName] = {users:{}};
		  }
	  } else {
		  if (!rooms.hasOwnProperty(roomName)) {
			  rooms[roomName] = {users:{}};
		  }
	  }
	  
	  
	  var time = (new Date()).toGMTString();
	  rooms[roomName].users[userId] = {"from":data.from,"time":time,socket:socket};
	  
	  socket.infos.roomName = roomName;
	  
	  socket.emit("connectRoom", {"from":"server","roomName":roomName,"query":data});
	  
	  console.log(data);
  });
  
  socket.on("disconnect", function(data) {
	  logoutRoom(socket);
	  console.log("Biri cikti");
  });
});

http.listen(port, function(){
  console.log('listening on - '+port);
});

/*
setInterval(function(){
	console.log(rooms);
},2000);*/