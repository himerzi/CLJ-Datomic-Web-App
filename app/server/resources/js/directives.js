'use strict';

/* Directives */


dactic.
directive('dacticCharge', [function() {
  return {
    scope: false,
    link: function(scope, element, attrs) {
      var handler = StripeCheckout.configure({
        image: '/img/clojure-logo.svg',
        key: "",
        name: "Dactic",
        description: "Course Title",
        amount: "£80",
        token: function(token, args) {
          console.log("behold! a token")
      // Use the token to create the charge with a server-side script.
    }
  });

      element.on('click', function(e) {
    // Open Checkout with further options
        handler.open({
          name: 'Demo Site',
          description: '2 widgets ($20.00)',
          amount: 2000
        });
        e.preventDefault();
      });
    }
  }
  }
  ]).
directive('script', function() {
  return {
    restrict: 'E',
    scope: false,
    link: function(scope, elem, attr) {
      if (attr.type=='text/javascript-lazy') {
        var code = elem.text();
        var f = new Function(code);
        f();
      }
    }
  };
}).
directive('dacticPopover', [function() {
    //Doesn't work on span elements, for some reason??
    function link(scope, element, attrs) {
      var $el = $(element);
      var content = attrs.dacticPopover;
      $el.addClass("dactic-popover");
      $el.attr({"data-toggle": "popover", "data-container": "body", "data-placement": "auto left", "data-content": content, "data-trigger": "hover"});
      $el.popover()
      element.on('$destroy', function() {
      });


    }

    return {
      link: link
    };
  }]).
  directive('dacticNoPropagate', [function() {
    function link(scope, element, attrs){
      element.on('click', function(event){
        event.stopPropagation();
      })
    }
    return{
      link: link
    }
  }]).
  directive('dacticIList', [function() {
    return {
      restrict: 'E',
      transclude: true,
      scope: {},
      templateUrl: 'my-dialog.html',
      link: function (scope, element) {
        scope.name = 'Jeff';
      }
    }
  }]);
