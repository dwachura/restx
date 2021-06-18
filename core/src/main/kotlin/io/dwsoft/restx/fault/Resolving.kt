package io.dwsoft.restx.fault;

typealias SingleCauseResolver<T> = (T) -> FaultCauseId<T>

typealias MultipleCauseResolver<T> = (T) -> Collection<FaultCauseId<T>>
