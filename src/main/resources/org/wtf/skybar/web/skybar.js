angular.module('skybar', [])
    .controller('SkybarController', ['$scope', '$interval', '$http', '$window', function ($scope, $interval, $http, $window) {
        $scope.sourceFiles = function () {
            var sourceFiles = [];
            for (var sourceFile in $scope.coverage) {
                sourceFiles.push(sourceFile);
                console.log(sourceFile);
            }
            return sourceFiles;
        };

        function updateCoverage() {
            $http.get(
                '/coverage.json'
            ).success(function (data) {
                    console.log("data: " + JSON.stringify(data));
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
    }])
    .controller('WebSocketController', ['$scope', '$window', function ($scope, $window) {
        var wsUri = "ws://localhost:4321/time/";

        var websocket = new WebSocket(wsUri);
        websocket.onopen = function (evt) {
            onOpen(evt)
        };
        websocket.onclose = function (evt) {
            onClose(evt)
        };
        websocket.onmessage = function (evt) {
            onMessage(evt)
        };
        websocket.onerror = function (evt) {
            onError(evt)
        };

        function onOpen(evt) {
            console.log("onOpen Event")
        };

        function onClose(evt) {
            console.log("onClose Event")
        };

        function onMessage(evt) {
            var parsed = JSON.parse(evt.data);
            $scope.time = parsed.time;
            $scope.$digest();

        };

        function onError(evt) {
            $window.alert("onError event")
        };

    }]);