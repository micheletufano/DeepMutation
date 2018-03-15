#!/usr/bin/env bash

# Check python
command -v python >/dev/null 2>&1 || 
    { echo >&2 "Please install python."; exit 1; }

# Check pip
command -v pip >/dev/null 2>&1 || 
    { echo >&2 "Please install pip."; exit 1; }

# Check tensorflow 1.0
echo "Checking tensorflow... "
tf_ver=`pip show tensorflow | grep Version | awk '{print $2}' \
                                           | sed 's/\(.*\..*\)\..*/\1/'`

if [ -z "$tf_ver" ] || [ ! "$tf_ver" = "1.0" ]; then
    echo >&2 "Please install tensorflow 1.0 using:
# pip install tensorflow==1.0"
    exit 1
fi

# Check seq2seq
echo "Checking seq2seq... "
s2s_ver=`pip show seq2seq | grep Version | awk '{print $2}'`

if [ -z "$s2s_ver" ] || [ ! "$s2s_ver" = "0.1" ]; then
    echo >&2 "Please install seq2seq 0.1 using:
# git clone https://github.com/google/seq2seq.git
# cd seq2seq
# pip install -e ."
    exit 1
fi

# Test seq2seq
echo "
To test seq2seq, run the following:
$ python -m unittest seq2seq.test.pipeline_test

An 'OK' indicates a successful test.

Please refer to https://github.com/google/seq2seq/issues/285 for:
    ImportError: cannot import name 'bernoulli'"
