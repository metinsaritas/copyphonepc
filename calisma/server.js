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

io.on('connection', function(socket){
  console.log('a user connected');
    
  socket.on("dataCopied", function(data){
	  if (typeof data != "object") return;
	  socket.broadcast.emit("otherCopied", data);
	  console.log(data);
  });
  
  /*
  socket.on("otherCopied", function(data){
	  if (typeof data != "object") return;
	  socket.broadcast.emit("phoneCopied", data); 
	  console.log(data);
  });
  */
});

http.listen(port, function(){
  console.log('listening onn:'+port);
});