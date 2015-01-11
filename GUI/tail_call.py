import functools


def tail_call(tuple_return=False):
    def __wrapper(func):

        def _optimize_partial(*args, **kwargs):
            """
            I replace the reference to the wrapped function with a functools.partial object
            so that it doesn't actually call itself upon returning, allowing us to do it instead.

            Advantages: Theoretically needs no code changes and is more understandable
            Disadvantages: Its startup overhead is higher and its a bit slower. Also can only call
                           recursively when returning, so return func(1) + func(2) will not work.
            """
            old_reference = func.func_globals[func.func_name]
            func.func_globals[func.func_name] = functools.partial(functools.partial, func)

            to_execute = functools.partial(func, *args, **kwargs)

            while isinstance(to_execute, functools.partial):
                to_execute = to_execute()

            func.func_globals[func.func_name] = old_reference
            return to_execute

        def _optimize_tuple(*args, **kwargs):
            """
            This way requires the function to return a tuple of arguments to be passed to the next
            call.

            Advantages: Very little overhead, faster than plain recursion
            Disadvantages: Needs code changes, not as readable, no support for keyword arguments (yet)
            """
            while args.__class__ is tuple:  # Faster than isinstance()!
            #while isinstance(args, tuple):
                args = func(*args)

            return args

        if tuple_return:
            functools.update_wrapper(_optimize_tuple, func)
            return _optimize_tuple
        else:
            functools.update_wrapper(_optimize_partial, func)
            return _optimize_partial

    return __wrapper


@tail_call()
def test_fib_optimize(i, current=0, next=1):
    if i == 0:
        return current
    else:
        return test_fib_optimize(i - 1, next, current + next)


@tail_call(tuple_return=True)
def test_fib_tuple_optimized(i, current=0, next=1):
    if i == 0:
        return current
    else:
        return i - 1, next, current + next,


def test_fib_no_optimize(i, current=0, next=1):
    if i == 0:
        return current
    else:
        return test_fib_no_optimize(i - 1, next, current + next)

import timeit
import sys
#sys.setrecursionlimit(8000)
#for func in (test_fib_optimize, test_fib_tuple_optimized, test_fib_no_optimize):
#    print(func.func_name, timeit.timeit(functools.partial(func, 1700), number=1000))
