// Provide a minimal process.env stub for browser test targets.
// Webpack 5 no longer polyfills `process` automatically, so any reference to
// `process.env` in the bundled code would throw a ReferenceError at runtime.
// DefinePlugin replaces `process.env` with a static object literal at bundle
// time, eliminating the reference entirely.
//
// A fixed, non-sensitive stub value for PATH is provided so that
// GetEnvTest.getenv_should_return_a_value_for_path_env_var passes in the
// browser runner without embedding any real process-environment secrets.
//
// The substitution string is parenthesised so the resulting `(...)` is
// always parsed as an expression. DefinePlugin's value is injected
// verbatim; without the surrounding parens, an injection like
//   process.env;  // statement
// would expand to
//   {"PATH":"/usr/bin:/bin"};
// which JavaScript parses as a block statement opener followed by a
// string-literal expression followed by `:`, producing
// "Uncaught SyntaxError: Unexpected token ':'" at the bundle's first
// statement-position reference to process.env. Wrapping in `(...)`
// forces the expression-statement reading so the object literal is
// unambiguous.
const webpack = require('webpack');

const envStub = JSON.stringify({ PATH: '/usr/bin:/bin' });

config.webpack = config.webpack || {};
config.webpack.plugins = (config.webpack.plugins || []).concat([
    new webpack.DefinePlugin({ 'process.env': '(' + envStub + ')' }),
]);
