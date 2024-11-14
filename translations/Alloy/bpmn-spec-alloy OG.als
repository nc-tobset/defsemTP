// Process
sig Process {
	name: lone String,
	flowNodes: set FlowNode,
	subProcesses: set SubProcess
}

// Flow node
abstract sig FlowNode {
	// sequence flows
	outgoingSequenceFlows: set SequenceFlow,
	incomingSequenceFlows: set SequenceFlow,
}

// Special flow nodes.
sig Task extends FlowNode {
}

sig StartEvent extends FlowNode {
}

sig EndEvent extends FlowNode {
}

sig ExGate extends FlowNode {
}

sig PaGate extends FlowNode {
}

sig IntermediateCatchEvent extends FlowNode {
}

sig IntermediateThrowEvent extends FlowNode {
}

sig TerminateEndEvent extends FlowNode {
}

sig EventBasedGateway extends FlowNode {
    outgoingEvents: set SequenceFlow
}

// Sequence flow
sig SequenceFlow {
	// Not needed atm.
	source: one FlowNode,
	target: one FlowNode
}

sig Event {
    trigger: set FlowNode
}

sig SubProcess extends FlowNode {
    subFlowNodes: set FlowNode,
    subStartEvent: one StartEvent,
    subEndEvents: set EndEvent  // Allows multiple end events
}

fact {
	// Make sequence flow source (target) symmetric to outgoingSequenceFlows (incomingSequenceFlows)
	all s:StartEvent {
		outSFs[s]
	}
	all a:Task {
		incSFs[a]
		outSFs[a]
	}
	all e:EndEvent {
		incSFs[e]
	}
}

pred incSFs(f:FlowNode) {
		f.incomingSequenceFlows.target = f
}

pred outSFs(f:FlowNode) {
		f.outgoingSequenceFlows.source = f
}

//Token sig + handling
var sig Token {
	var pos: one StartEvent + Task + SequenceFlow + IntermediateCatchEvent + IntermediateThrowEvent + EventBasedGateway
}

sig ProcessSnapshot {
	var tokens: set Token
}



pred startEventHappens(ps: ProcessSnapshot, s:StartEvent, t:Token) {
	-- pre conditions
	t in ps.tokens
	t.pos = s
	-- post condition
	some newToken : Token'-Token {
		Token' = Token + newToken - t
		//newToken.pos' = s.outgoingSequenceFlows
		pos' = pos + newToken->s.outgoingSequenceFlows - t->s
		tokens' = tokens  + ps->newToken - ps->t
	}
	-- frame conditions
}

pred taskStart(ps: ProcessSnapshot, a:Task, sf:SequenceFlow) {
	-- pre conditions
	one t: ps.tokens {
		t.pos = sf and sf.target = a
		-- post conditions
		some newToken : Token'-Token {
			Token' = Token + newToken - t
			pos' = pos + newToken->a - t->sf
			tokens' = tokens  + ps->newToken - ps->t
		}
	}
	-- frame conditions
}

pred taskEnd(ps: ProcessSnapshot, a:Task, sf:SequenceFlow) {
	one t: ps.tokens {
		t.pos = a
		some newToken : Token'-Token {
			Token' = Token + newToken - t
			pos' = pos + newToken->a.outgoingSequenceFlows - t->a
			tokens' = tokens  + ps->newToken - ps->t
		}
	}
}

pred exGateHappens (ps: ProcessSnapshot, x:ExGate, isf:SequenceFlow, osf:SequenceFlow) {
	-- pre conditions
	one t: ps.tokens {
		t.pos = isf and isf.target = x
		osf in x.outgoingSequenceFlows
		-- post conditions
		some newToken : Token'-Token {
			Token' = Token + newToken - t
			pos' = pos + newToken->osf - t->isf
			tokens' = tokens  + ps->newToken - ps->t
		}
	}
}

pred paGateHappens (ps: ProcessSnapshot, p:PaGate) {
	-- pre conditions
	let oldTokens = {t: Token | t.pos in p.incomingSequenceFlows} {
		#oldTokens.pos = #p.incomingSequenceFlows

		--post conditions
		let newTokens = Token' - Token {
			Token' = Token + newTokens - oldTokens
			(Token - oldTokens).pos' = (Token - oldTokens).pos
			tokens' = tokens  + ps->newTokens - ps->oldTokens
			newTokens.pos' = p.outgoingSequenceFlows
			#newTokens = #p.outgoingSequenceFlows
		}
	}
}

pred intermediateCatchEventStarts(ps: ProcessSnapshot, ice: IntermediateCatchEvent, sf: SequenceFlow) {
    -- pre conditions
    one t: ps.tokens {
        t.pos = sf and sf.target = ice
        -- post conditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->ice - t->sf
            tokens' = tokens  + ps->newToken - ps->t
        }
    }
}
pred intermediateCatchEventHappens(ps: ProcessSnapshot, ice: IntermediateCatchEvent, osf: SequenceFlow) {
    -- preconditions
    one t: ps.tokens {
        t.pos = ice
        osf in ice.outgoingSequenceFlows
        -- post conditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->ice.outgoingSequenceFlows - t->ice
            tokens' = tokens + ps->newToken - ps->t
        }
    }
}

pred intermediateThrowEventStarts(ps: ProcessSnapshot, ite: IntermediateThrowEvent, sf: SequenceFlow) {
    -- pre conditions
    one t: ps.tokens {
        t.pos = sf and sf.target = ite
        -- post conditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->ite - t->sf
            tokens' = tokens  + ps->newToken - ps->t
        }
    }
}
pred intermediateThrowEventHappens(ps: ProcessSnapshot, ite: IntermediateThrowEvent, osf: SequenceFlow) {
    -- preconditions
    one t: ps.tokens {
        t.pos = ite
        osf in ite.outgoingSequenceFlows
        -- post conditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->ite.outgoingSequenceFlows - t->ite
            tokens' = tokens + ps->newToken - ps->t
        }
    }
}

pred endEventHappens(ps: ProcessSnapshot, e:EndEvent, sf:SequenceFlow) {
	-- pre conditions
	one t: ps.tokens {
		sf in t.pos and sf.target = e
		-- post conditions
		Token' = Token - t
		pos' = pos - t->sf
		tokens' = tokens - ps->t
	}
	-- frame conditions
}

pred terminateEndEventHappens(ps: ProcessSnapshot, te:TerminateEndEvent, sf:SequenceFlow) {
	-- pre conditions
	one t: ps.tokens {
		sf in t.pos and sf.target = te
		-- post conditions
		-- remove all tokens as the process terminates immediately when this point is reached.
		ps.tokens' = ps.tokens - ps.tokens
	}
	-- frame conditions
}

pred eventBasedGatewayStarts(ps: ProcessSnapshot, ebg: EventBasedGateway, sf: SequenceFlow) {
    -- preconditions
    one t: ps.tokens {
        t.pos = sf and sf.target = ebg
        -- postconditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->ebg - t->sf
            tokens' = tokens + ps->newToken - ps->t
        }
    }
}

pred eventBasedGatewayHappens(ps: ProcessSnapshot, ebg: EventBasedGateway, osf: SequenceFlow, ev: Event) {
    -- preconditions
    one t: ps.tokens {
        t.pos = ebg
        osf in ebg.outgoingEvents
        ev.trigger = osf.target
        -- postconditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->osf - t->ebg
            tokens' = tokens + ps->newToken - ps->t
        }
    }
}

pred enterSubProcess(ps: ProcessSnapshot, sp: SubProcess, sf: SequenceFlow) {
    -- Preconditions
    one t: ps.tokens {
        t.pos = sf and sf.target = sp
        -- Postconditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t
            pos' = pos + newToken->sp.subStartEvent - t->sf
            tokens' = tokens + ps->newToken - ps->t
        }
    }
}

pred exitSubProcess(ps: ProcessSnapshot, sp: SubProcess, spEndEvent: EndEvent, incomingFlow: SequenceFlow, outgoingFlow: SequenceFlow) {
    -- Preconditions
    one t: ps.tokens {
        incomingFlow.target = spEndEvent                // The incoming flow leads to the subprocess end event
        t.pos = incomingFlow                            // Token is on the incoming flow of the end event
        outgoingFlow in sp.outgoingSequenceFlows        // outgoingFlow is a valid outgoing flow from the subprocess
        
        -- Postconditions
        some newToken : Token'-Token {
            Token' = Token + newToken - t               // Remove the token from incomingFlow, add it on outgoingFlow
            pos' = pos + newToken->outgoingFlow - t->incomingFlow
            tokens' = tokens + ps->newToken - ps->t     // Update tokens in the ProcessSnapshot
        }
    }
}


pred terminateCheck(ps: ProcessSnapshot) {
	#ps.tokens = 0
}

pred terminates[] {
	(some ps: ProcessSnapshot | terminateCheck[ps])
}

pred unsafe[] {
	(some t,t1 : Token | unsafeHelper[t,t1])
}

pred unsafeHelper(t,t1 : Token) {
	t != t1
	t.pos = t1.pos
}

one sig startEvent1 extends StartEvent {}
one sig startEvent2 extends StartEvent {}
one sig task1 extends Task {}
one sig exclusiveGateway1 extends ExGate {}
one sig endEvent1 extends EndEvent {}

pred init {
    startEvent1.outgoingSequenceFlows.target = task1
    task1.outgoingSequenceFlows.target = exclusiveGateway1
    exclusiveGateway1.outgoingSequenceFlows.target = endEvent1

    one t: ProcessSnapshot.tokens | t.pos = startEvent1
}

pred trans [] {
	(some ps: ProcessSnapshot, s: StartEvent, t:Token | startEventHappens[ps, s, t])
	or
	(some ps: ProcessSnapshot, a: Task, sf:SequenceFlow | taskStart[ps, a, sf])
	or
	(some ps: ProcessSnapshot, a: Task, sf:SequenceFlow | taskEnd[ps, a, sf])
	or
	(some ps: ProcessSnapshot, x: ExGate, isf,osf:SequenceFlow | exGateHappens[ps, x, isf, osf])
	or
	(some ps: ProcessSnapshot, p: PaGate | paGateHappens[ps, p])
	or
	(some ps: ProcessSnapshot, e: EndEvent, sf:SequenceFlow | endEventHappens[ps, e, sf])
    	or
    	(some ps: ProcessSnapshot, ice: IntermediateCatchEvent, sf: SequenceFlow | intermediateCatchEventStarts[ps, ice, sf])
    	or
    	(some ps: ProcessSnapshot, ice: IntermediateCatchEvent, osf: SequenceFlow | intermediateCatchEventHappens[ps, ice, osf])
    	or
    	(some ps: ProcessSnapshot, ite: IntermediateThrowEvent, sf: SequenceFlow | intermediateThrowEventStarts[ps, ite, sf])
    	or
    	(some ps: ProcessSnapshot, ite: IntermediateThrowEvent, osf: SequenceFlow | intermediateThrowEventHappens[ps, ite, osf])
    	or
	(some ps: ProcessSnapshot, te: TerminateEndEvent, sf: SequenceFlow | terminateEndEventHappens[ps, te, sf])
	or
    	(some ps: ProcessSnapshot, ebg: EventBasedGateway, sf: SequenceFlow | eventBasedGatewayStarts[ps, ebg, sf])
    	or
    	(some ps: ProcessSnapshot, ebg: EventBasedGateway, osf: SequenceFlow, ev: Event | eventBasedGatewayHappens[ps, ebg, osf, ev])
    	or
    	(some ps: ProcessSnapshot, sp: SubProcess, sf: SequenceFlow | enterSubProcess[ps, sp, sf])
    	or
    	(some ps: ProcessSnapshot, sp: SubProcess, spEndEvent: EndEvent, incomingFlow: SequenceFlow, outgoingFlow: SequenceFlow | exitSubProcess[ps, sp, spEndEvent, incomingFlow, outgoingFlow])
    	or
	doNothing
}

pred doNothing [] {
	-- the relevant relations stay the same
	tokens'=tokens
	Token'=Token
	pos'=pos
}

pred System {
	init and always trans-- and eventually terminates and not eventually unsafe
}

run System for 5 Token, 6 FlowNode, 4 SequenceFlow, 1 Process, 2 StartEvent, 2 EndEvent, 1 ProcessSnapshot, 1 SubProcess, 1 Task, 0 Event
