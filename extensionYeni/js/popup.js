﻿var panelSettings;
var pages;

var port = chrome.extension.connect({
      name: "iletisim"
 });

const Status = new function () {
    this.CONNECT = 0;
    this.CONNECTING = 1;
    this.DISCONNECT = 2;
};

const Toast = new function () {
    this.makeText = function (text, time, compile, scope) {
        text = text.split("<").join("&lt;");
        
        $("body").append(compile('<android-toast duration="'+time+'" text="'+text+'"/>"')(scope));
    }

    this.LENGTH_SHORT = 500;
    this.LENGTH_LONG = 800;
};

$(function() {
    panelSettings = $("[app-panel=settings]");
    pages = $("#header .contentHolder .page");
    $(document).on("click","#header .tabHolder .tab", function() {
    
    });

    $(document).on("click",".contentHolder .page[page-name!=main] ul li", function(event) {
        if ($(event.target).is("a")) return;

        var a = $(this).find("a[href]");
        $(a).click();
    });

    $(document).on("contextmenu", function(){
        return true;
    });

    $(document).on("paste", function(e){ 
        if($(e.target).is("#tvRoomName")) {
            //TODO: bura copy elemen ile cagir paste element ile cagir falan paste sonsuz dongu olabilir dikkat
        }
    });

    
});




function getAngular(query) {
    return angular.element(query ? query : "#bodyHolder");
}

function getScope(query) {
    if (query)  return getAngular(query).scope();
                return getAngular().scope();
}

function URLTester(text) {
    var startsW = !(text.startsWith("http://") || text.startsWith("https://"));
    if (startsW) text = "http://" + text;

    var res = text.match(/(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/g);
    if(res == null)
        return {result:false};
    else
        return {result:true, text:text};
}





function readFromCache () {
    if (!("localStorage" in window)) return;
    try {
        var roomName                = local("roomName");            if (roomName)        getScope().roomName         = roomName;
        var copiedList              = local("copiedList");          if (copiedList)      getScope().copiedList       = JSON.parse(copiedList);
        var nick                    = local("nick");                if (nick)            getScope().nick             = nick;
        var statusGetRemote         = local("statusGetRemote");     if (statusGetRemote) getScope().statusGetRemote  = eval(statusGetRemote);
        var statusSetRemote         = local("statusSetRemote");     if (statusSetRemote) getScope().statusSetRemote  = eval(statusSetRemote);
    } catch (e) {
        localRemove();
    }
    
    //getScope().$apply();
}

function local(key, value) {
    if (!key) return;
    if (!("localStorage" in window)) return;
    switch (arguments.length) {
        case 1: return localStorage.getItem(key);
        break;
        case 2: localStorage.setItem(key, value);
        break;
    }
}

function localRemove(key) {
    if (!("localStorage" in window)) return;
    if (!key) localStorage.clear();

    localStorage.removeItem(key);
}

var app = angular.module("main", []);
app.controller("controller", function($scope, $compile, $sce) {
    

    $scope.Settings = {appName:"Copy Team", colorPrimary: "#2d3e50", colorSecond: "#ffa500"};
    $scope.statusPanelSettings = false;
    $scope.selectedTab = Number(local("selectedTab") || 1);
    $scope.copiedList = [];
    $scope.userList = [];

    $scope.roomName = "";

    $scope.roomStatus = Status.CONNECT;
    $scope.connectText = ["Connect","Connecting","Disconnect"];
    $scope.selectTab = function (val) {
        $scope.selectedTab = val;
    };

    $scope.nick = "User";
    $scope.copiedLimit = 20;
    $scope.statusGetRemote = true;
    $scope.statusSetRemote = true;

    readFromCache();

    $(document).on("contextmenu",".page[page-name=lastcopied] ul li", function() {
        
        var index = $(this).index();
        $scope.copy(null, $scope.copiedList[index].copiedText);
        Toast.makeText("Copied", Toast.LENGTH_SHORT, $compile, $scope);
        if (index == 0) return false;

        var copiedObject = $scope.copiedList[index];
        $scope.copiedList = $scope.copiedList.filter(function(data,i) {
            return i != index;
        });
        
        //$scope.copiedList.unshift(copiedObject);
        //$scope.$apply();
        return false;
    });

    $scope.$watch("statusPanelSettings", function(n,o) {
        $(panelSettings).css({
            top:0,
            left:0,
            display:"block"
        });

        var content = $(panelSettings).find(".content");
        $(content).animate({
            left: n?"20%":"100%"
        },300, function() {
            $(panelSettings)[n?"show":"hide"]();
        });
    });

    $scope.showPanelSettings = function () {
        $scope.statusPanelSettings = true;
    };

    
    //$scope.showPanelSettings(); // TODO: delete this line

    $scope.closePanelSettings = function () {
        $scope.statusPanelSettings = false;
    };

    $scope.$watch("selectedTab", function(n,o) {
        if (pages == undefined) return;

        var direction = n > o ? "-":"+";

        var pagesCount = pages.length;
        for (i = 0; i < pagesCount; i++) {
            var page = $(pages[i]);
            var index = page.index();
            page.animate({
                left: direction+"=350px"
            });
        }

        local("selectedTab", n);
    });

    $scope.copy = function (e,t) {
        var element = $(e);
        var text = t ? t : element.text();
        var sandbox = $('#hiddenClipboard').val(text).select();
        document.execCommand('copy');
        sandbox.val("");
        element.text("");
        Toast.makeText("Copied", Toast.LENGTH_LONG, $compile, $scope);
    };
    
    $scope.paste = function (e) {
        var hiddenClipboard = $("#hiddenClipboard");
        hiddenClipboard.val("");
        hiddenClipboard.focus();
        document.execCommand("paste");
        var text = hiddenClipboard.val();
        $(e).text(text);
        
    };

    $scope.roomOperation = function () {
        if ($scope.roomStatus == Status.CONNECT) {
            $scope.connectRoom();
        } else if ($scope.roomStatus == Status.DISCONNECT) {
            $scope.disconnectRoom();
        }
    };

    $scope.connectRoom = function () {
        $scope.roomName = angular.element("#tvRoomName").text();
        var request = {type:"connectRoom",data:{"from":"chrome","roomName":$scope.roomName,"hasRoom":false,"name":$scope.nick}};
        $scope.roomStatus = Status.CONNECTING;
        port.postMessage(request);
    };

    $scope.disconnectRoom = function () {
        var request = {type:"disconnectRoom"};
        port.postMessage(request);
        cbDisconnectRoom();
    };

    $scope.clearList = function () {
        localRemove("copiedList");
        $scope.copiedList = [];
    };

    $scope.applyChangeName = function (n) {
        var nick = n || $("#tvNick").text() || "";
        local("nick", nick);
        $scope.nick = nick;
        Toast.makeText("Applying", Toast.LENGTH_SHORT, $compile, $scope);
    };

    $scope.applyChangeCopiedLimit = function (n) {
        var copiedLimit = Number(n || $("#tvCopiedLimit").text() || 20);
        copiedLimit = copiedLimit > 0 ? copiedLimit : 20;
        local("copiedLimit", copiedLimit);
        $scope.copiedLimit = copiedLimit;
        
        Toast.makeText("Applying", Toast.LENGTH_SHORT, $compile, $scope);
    };

    $scope.$watch("nick", function (n,o) {
        if (n == o) return;

        var request = {type:"changeName", data:{"name":n}};
        port.postMessage(request);
    });

    $scope.changeStatusGetRemote = function () {
        var element = $("#tbGetRemote");
        var status = element.attr("status");
        $scope.statusGetRemote = eval(status);
    };

    $scope.changeStatusSetRemote = function () {
        var element = $("#tbSetRemote");
        var status = element.attr("status");
        $scope.statusSetRemote = eval(status);
    };

}).directive("androidButton", function(){
    return {
        link: function (scope, element, attr) {
            element.addClass("noselect");
        }
    }
}).directive("androidTextview", function(){
    return {
        link: function(scope, element, attr) {
            element.attr("contenteditable",true);
        }
    }
}).directive("androidTextview", function(){
    return {
        restrict: 'E',
        link: function(scope, element, attr) {
            element.attr("contenteditable",true);
            element.on("focus", function(event){
                element.css({"border-bottom-color": scope.Settings.colorSecond});    
            });

            element.on("focusout", function(event){
                element.css({"border-bottom-color": scope.Settings.colorPrimary}); 
            });
        }
    }
}).directive("androidToast", function(){
    return {
        template: function (element, attr) {
            var text = "text" in attr ? attr.text : "";
            return text
        },
        link: function (scope, element, attr) {
            var duration = "duration" in attr ? attr.duration : Toast.LENGTH_SHORT;
            

            setTimeout(function(){
                $(element).fadeIn("slow", function(){

                    var width = $(element).width() / 2;
                    $(element).css({
                        "margin-left": -width+"px"
                    });

                    setTimeout(function(){
                        $(element).fadeOut("slow", function(){
                            $(element).remove();
                        });
                    }, duration);
                });
            },100);
        }
    }
}).directive("androidTogglebutton", function () {
    return {
        restrict: "E",
        template: function (element, scope) {
            var html = '<div>{{text}}</div><div class="toggleButtonStatus" ng-class="status && '+"'checked'"+'"></div>';
            return html;
        },
        scope: {
            status:"@androidTogglebutton"
        },
        link: function (scope, element, attr) {

            scope.status = "status" in attr ? attr.status : false;
            scope.texton = "texton" in attr ? attr.texton : "Active";
            scope.textoff = "textoff" in attr ? attr.textoff : "Passive";
            scope.watcher = "watcher" in attr ? attr.watcher : null;
            scope.text = scope.status ? scope.texton : scope.textoff;
            
            element.on("click", function(event) {
                scope.status = !scope.status;
                scope.$apply();
            });

            scope.$watch("status", function(n,o) {
                  scope.text = n ? scope.texton : scope.textoff;
                  if (scope.watcher && scope.watcher in scope.$parent) {
                      scope.$parent[scope.watcher] = eval(n);
                    }
            });

            scope.$parent.$watch(scope.watcher, function(n,o) {
                scope.text = attr.status == "true" ? scope.texton : scope.textoff;
                local(scope.watcher, n);
            });
        }
    }
});

port.onMessage.addListener(function(request) {
    if (eval("typeof "+request.callback+" == 'function'")) {
        eval(request.callback+"("+JSON.stringify(request)+");");
    }
});

function cbConnectRoom (data) {
    local("roomName", getScope().roomName);
    getScope().roomStatus = Status.DISCONNECT;
    getScope().$apply();
    
}

function cbDisconnectRoom() {
    localRemove("roomName");
    getScope().roomStatus = Status.CONNECT;
    //getScope().$apply();
}

function cbRoomUpdated (data) {
    console.log(data);
    getScope().userList = data.users;
    getScope().$apply();
}

function cbScreen (data) {
    if (data.roomStatus) {
        getScope().roomStatus = data.roomStatus; 
        if(data.roomStatus != 2) getScope().selectTab(1);
    }
    else {
        getScope().selectTab(1);
    }

    if (data.room) getScope().userList = data.room.users;
    getScope().$apply();
}

function cbDataCopied (data) {
    console.log("cbDataCopied kismi");
    console.log(data);
    if (!("data" in data)) return;
    var copied = data.data.copiedText;
    var ret = URLTester(copied);
    if (ret.result) {
        data.data.textLink = ret.text;
        data.data.type = 'LINK';
    }
    getScope().copiedList.unshift(data.data);

    var copiedList = [];
    var list = getScope().copiedList;
    var limit = getScope().copiedLimit;
    list = list.slice(0, limit);
    list.forEach(function(val, index){
        var withoutHashKey = val;
        delete withoutHashKey["$$hashKey"];
        copiedList.push(withoutHashKey);
    });

    
    getScope().copiedList = copiedList;
    local("copiedList", JSON.stringify(copiedList));
    getScope().$apply();
}