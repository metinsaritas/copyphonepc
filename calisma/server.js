const express = require("express");
var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);

var port = process.env.PORT || 3000;
const MIN_DIGIT_LENGTH = 6;
app.use(express.static("public"));

app.get('/', function(req, res){
  res.sendFile('/index.html');
});

//

const digitChars = ["A","B","C","D","E","F","G","H","J","K","L","M","N","P","R","S","T","U","W","X","V","Z","1","2","3","4","5","6","7","8","9"];
const connectRoomKeys = Object.keys({"from":null,"roomName":null,"hasRoom":null,"name":null});
const changeNameKeys = Object.keys({"name":null});
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
	var digitLength = MIN_DIGIT_LENGTH;
	var digit = "000000";
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
				var nextAdmin = Object.keys(rooms[socket.infos.roomName].users);
				rooms[socket.infos.roomName].users[nextAdmin].admin = true;
				//rooms[socket.infos.roomName].users[nextAdmin].socket.infos = true;//hata verdi bi araf
			}
			
			roomUpdate(socket.infos.room.users);
		}
	}
	
	socket.infos.roomName = null;
	socket.infos.room = null;
	//socket.infos.admin = false;
	
}


io.on('connection', function(socket){
  console.log('a user connected');
  
  var userId = giveDigit(2) + Number(new Date()).toString();
  socket.infos = {roomName:null,room:null,userId:userId};
    
  socket.on("dataCopied", function(data){
	  if (dataError(data,socket)) return;
	  
	  console.log("Bir veri kopyalanadi");
	  console.log(data);
	  
	  //bura hatli gibi-cozdum sanirim
	  if ((socket.infos.roomName in rooms)) {
	  
		  var userOnRoom = Object.keys(rooms[socket.infos.roomName].users);
		  userOnRoom.forEach(function(val,index) {
			  if (val == socket.infos.userId)
				  return;
			  var roomSocket = rooms[socket.infos.roomName].users[val].socket;
			  data.sender = socket.infos.userId;
			  
			  roomSocket.emit("otherCopied", data);
		  });
	  }
  });
  
  socket.on("connectRoom", function(data){
	  if (dataError(data,socket)) return;
	  if (jsonError(data, connectRoomKeys, socket)) return;
	  var roomName = data.roomName;
	  var admin = false;
	  if (socket.infos.roomName) {
		  logoutRoom(socket);
	  }
	  
	  if (!data.hasRoom) {
		  if (!rooms.hasOwnProperty(roomName)) {
			  roomName = createRoom();
			  admin = true;
			  console.log("Different room created");
			  rooms[roomName] = {users:{}};
		  }
	  } else {
		  if (!rooms.hasOwnProperty(roomName)) {
			  admin = true;
			  console.log("Old room created");
			  rooms[roomName] = {users:{}};
		  }
	  }
	  
	  
	  var time = (new Date()).toGMTString();
	  rooms[roomName].users[userId] = {"admin":admin,"from":data.from,"time":time,"socket":socket,"name":data.name};
	  
	  socket.infos.room = rooms[roomName];
	  socket.infos.roomName = roomName;
	  socket.infos.name = data.name;
	  socket.infos.admin = admin;
	  
	  var result = {"from":"server","roomName":roomName,"query":data,"user":null};
	  result.user = {"admin":admin,"from":data.from,"time":time,"name":data.name,"userId":userId};
	  socket.emit("connectRoom", result);
	  roomUpdate(socket.infos.room.users);
	  
	  //console.log(data);
  });
  
  socket.on("disconnect", function(data) {
	  logoutRoom(socket);
	  console.log("Biri cikti");
  });
  
  socket.on("disconnectRoom", function(data) {
	  logoutRoom(socket);
  });
  
  socket.on("changeName", function(data) {
	if (dataError(data,socket)) return;
	if (jsonError(data, changeNameKeys, socket)) return;
	
	socket.infos.name = data.name;
	socket.infos.room.users[socket.infos.userId].name = data.name;
	roomUpdate(socket.infos.room.users);
  });
});

http.listen(port, function(){
  console.log('listening on - '+port);
});

function roomUpdate (roomUsers) {
	var withoutSocket = {"namelist":[],"users":[]};
	
	var users = Object.keys(roomUsers);
	users.forEach(function(val,index){
		var data = roomUsers[val];
		withoutSocket.users.push({"admin":data.admin,"id":val,"from":data.from,"time":data.time,"Name":data.name});
		withoutSocket.namelist.push(val);
	});
	
	users.forEach(function(val,index){
		var socket = roomUsers[val].socket;
		socket.emit("roomUpdated", withoutSocket);
	});
}

/*
setInterval(function(){
	console.log(rooms);
},2000);*/