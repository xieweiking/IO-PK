TCN_IMPLEMENT_CALL(jint, Poll, interrupt)(TCN_STDARGS, jlong pollset)
{
    tcn_pollset_t *p = J2P(pollset,  tcn_pollset_t *);
    apr_status_t rv = APR_SUCCESS;
    rv = apr_pollset_wakeup(p->pollset);
    return (jint) rv;
}
