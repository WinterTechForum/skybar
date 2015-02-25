angular.module('skybar', [])
    .controller('SkybarController', ['$scope', '$interval', '$http', '$window', function ($scope, $interval, $http, $window) {
        $scope.sourceFiles = function () {
            var sourceFiles = [];
            for (var sourceFile in $scope.coverage) {
                sourceFiles.push(sourceFile);
                //console.log(sourceFile);
            }
            return sourceFiles;
        };


        $scope.loadSource = function (sourceFile) {
            console.log("sourceFile = " + sourceFile)
            $http.get(
                    '/source/' + sourceFile
            ).success(function (data) {

                    var sourceLines = data.split("\n")
                    $scope.sourceLines = []

                    for (var lineNum in sourceLines) {

                        var lineNumKey = (lineNum - 0 + 1).toString();
                        var execCount = $scope.coverage[sourceFile][lineNumKey] || 0

                        console.log("execCount for " + lineNumKey + " = " + execCount)


                        $scope.sourceLines.push({ "text": sourceLines[lineNum], "execCount": execCount })
                    }
                    $scope.currentSourceFile = sourceFile

                    console.log($scope.sourceLines)
                }).error(function (data, status) {
                    console.log("error loading coverage data:");
                    console.log("status: " + status);
                    console.log("data: " + data);
                    $window.alert("Error loading coverage data.")
                })
        }

        $scope.getExecCount = function (coverage, sourceFile, lineNumber) {
            var sourceFileCoverage = coverage[sourceFile]
            return sourceFileCoverage[lineNumber.toString()]
        }
        function updateCoverage() {
            $http.get(
                '/coverage.json'
            ).success(function (data) {
                    //console.log("data: " + JSON.stringify(data));
                    $scope.coverage = data
                }).error(function (data, status) {
                    console.log("error loading coverage data:");
                    console.log("status: " + status);
                    console.log("data: " + data);
                    $window.alert("Error loading coverage data.")
                })
        }

        updateCoverage();
        $interval(updateCoverage, 2000);


    }]).controller('WebSocketController', ['$scope', '$window', function ($scope, $window) {
        var host = location.host;
        var wsUri = "ws://" + host + "/time/";
        var websocket = new WebSocket(wsUri);

        websocket.onopen = function (evt) {
            console.log("onOpen Event")
        };
        websocket.onclose = function (evt) {
            console.log("onClose Event")
        };
        websocket.onmessage = function (evt) {
            var parsed = JSON.parse(evt.data);
            $scope.time = parsed.time;
            $scope.$digest();
        };
        websocket.onerror = function (evt) {
            $window.alert("onError event")
        };

    }]);