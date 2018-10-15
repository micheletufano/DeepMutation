#!/usr/bin/env bash

# Check that we are running this (not sourcing) so that exit clauses are safe
if [[ "$(basename -- "$0")" != "setup_env.sh" ]]; then
    echo "Don't source setup_env.sh, run it"
# The rest of program is within the else close
else

# Check python
command -v python >/dev/null 2>&1 && python --version 2>&1 | grep -q " 3.[0-6]" || 
    { echo >&2 "Please install python version 3.0.* through 3.6.*."; exit 1; }

# Check pip
command -v pip >/dev/null 2>&1 || 
    { echo >&2 "Please install pip."; exit 1; }

# Check tensorflow 1.3
echo "Checking tensorflow... "
tf_ver=`pip show tensorflow | grep Version | awk '{print $2}' \
                                           | sed 's/\(.*\..*\)\..*/\1/'`

if [ -z "$tf_ver" ] || [ ! "$tf_ver" = "1.3" ]; then
    echo >&2 "Please install tensorflow 1.3 using:
> pip install [--user] tensorflow==1.3.0"
    # exit 1
fi

# Check seq2seq
echo "Checking seq2seq... "
s2s_ver=`pip show seq2seq | grep Version | awk '{print $2}'`

if [ -z "$s2s_ver" ] || [ ! "$s2s_ver" = "0.1" ]; then
    echo >&2 "Please install seq2seq 0.1 using:
> git clone https://github.com/google/seq2seq.git
> cd seq2seq
> pip install [--user] -e ."
    # exit 1
fi

# Test seq2seq
echo "Testing seq2seq... "
ok_status=`python -m unittest seq2seq.test.pipeline_test 2>&1 | grep OK`

if [ -z "$ok_status" ]; then
    echo >&2 "
ERROR:
Please test seq2seq manually using:
> python -m unittest seq2seq.test.pipeline_test

An 'OK' indicates a successful test.

If the seq2seq module is not found, please check that pip and python are
on the correct version.

A common issue related to helper.py can be addressed by following these steps:
  emacs seq2seq/contrib/seq2seq/helper.py
  Replace the import lines for bernoulli and categorical with these:
    from tensorflow.contrib.distributions import Bernoulli
    from tensorflow.contrib.distributions import Categorical

Another common issue relates to $DISPLAY not being correct. This may be a matplotlib issue best resolved with:
    echo "backend : Agg" >> ${XDG_CACHE_HOME:-$HOME/.cache}/matplotlib/matplotlibrc


Please refer to https://github.com/google/seq2seq/issues/285 for ImportErrors."
    exit 1
else 
    echo "Success."
fi

# End of 'source' check
fi
