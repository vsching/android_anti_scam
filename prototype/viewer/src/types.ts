export type ScreenStatus = 'built' | 'planned' | 'future';
export type FlowType = 'forward' | 'back' | 'error' | 'success';

export interface ScreenDefinition {
  id: string;
  name: string;
  epic: string;
  status: ScreenStatus;
  position: { x: number; y: number };
  states: string[];
  group?: string;
}

export interface Flow {
  from: string;
  to: string;
  action: string;
  type: FlowType;
}
