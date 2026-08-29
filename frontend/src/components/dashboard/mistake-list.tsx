import type { MistakePattern } from '@revexa/core';
import { CircleCheckIcon } from 'lucide-react';

import { EmptyState } from '@/components/empty-state';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export function MistakeList({ mistakes }: { mistakes: MistakePattern[] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Recurring mistakes</CardTitle>
        <CardDescription>The findings that keep coming back across your reviews.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {mistakes.length === 0 ? (
          <EmptyState
            icon={CircleCheckIcon}
            title="Nothing repeating yet"
            description="Once the same finding appears in more than one review, it shows up here with coaching."
            className="border-0 py-6"
          />
        ) : (
          mistakes.map((mistake) => (
            <div key={mistake.title} className="rounded-lg border p-3">
              <div className="flex items-start justify-between gap-3">
                <p className="text-sm font-medium text-pretty">{mistake.title}</p>
                <Badge variant={mistake.severity === 'HIGH' ? 'risky' : mistake.severity === 'MEDIUM' ? 'improvable' : 'muted'}>
                  {mistake.occurrences}×
                </Badge>
              </div>
              <p className="text-muted-foreground mt-1.5 text-xs leading-relaxed text-pretty">{mistake.coaching}</p>
            </div>
          ))
        )}
      </CardContent>
    </Card>
  );
}
