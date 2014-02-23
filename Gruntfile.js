module.exports = function(grunt) {

  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),
    concat: {
      options: {
        separator: ';'
      },
      dev: {
        src: ['bower_components/angular/angular.js','bower_components/angular-route/angular-route.js','bower_components/angular-resource/angular-resource.js',
        'bower_components/jquery/jquery.js',
        'bower_components/bootstrap/dist/js/bootstrap.js'],
        dest: 'app/server/resources/lib/js/vendor.js'
      }
    },
    less: {
        dev: {
            files: {
                "app/client/lib/css/vendor.css": "bower_components/bootstrap/dist/css/bootstrap.min.css"
            }
        }
    },
    copy: {
      dev: {
        files: [
          {expand: true, flatten: true, src: ['bower_components/bootstrap/dist/fonts/*'], dest: 'app/client/lib/fonts/'}

        ]
      },
      dist: {
        files: [
          {expand: true, cwd:'app/server/', src: ['**'], dest: 'dist/'}

        ]
      }
    },
    buildcontrol: {
        options: {
          dir: 'dist',
          commit: true,
          message: 'Built %sourceName% from commit %sourceCommit% on branch %sourceBranch%'
        },
        heroku: {
          options: {
            remote: 'git@heroku.com:mysterious-bayou-1592.git',
            branch: 'master',
            dir: "dist",
            push: true
          }
        },
        local: {
          options: {
            remote: '../',
            branch: 'build'
          }
        }
      },
    watch: {
      files: [],
      tasks: []
    }
  });

  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-concat');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-build-control');
  grunt.registerTask('build-vendor', ['concat:dev', 'less:dev', 'copy:dev']);
  grunt.registerTask('build-dist', ['copy:dist', 'buildcontrol:heroku']);

  grunt.registerTask('default', []);

};
